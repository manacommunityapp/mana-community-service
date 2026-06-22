# Logging, Auditing & Monitoring — Framework & Best-Practices Guide

This document describes the enterprise observability framework for the Mana Community
backend (Spring Boot 3.x / PostgreSQL / JWT) and the rules every contributor must follow.

> TL;DR: every request is correlated, sensitive data is masked, and every sensitive
> mutation is audited to the database. Use `AuditService` for business audit, the named
> loggers for module logs, and **never** log secrets.

---

## 1. Architecture at a glance

```
            ┌─────────────────────────── HTTP request ───────────────────────────┐
            │                                                                     │
   CorrelationIdFilter (HIGHEST_PRECEDENCE)                                       │
     • X-Correlation-Id (read or generate) → MDC[correlationId]                   │
     • echoes id on response header; clears MDC in finally                        │
            │                                                                     │
   RequestResponseLoggingFilter (+10)                                             │
     • "--> METHOD /uri?maskedQuery ip=…"  /  "<-- … status= durationMs= size="   │
     • slow-API: WARN ≥2s, ERROR ≥5s  → PERFORMANCE logger                        │
            │                                                                     │
   JwtAuthenticationFilter → sets MDC[userId] once authenticated                  │
            │                                                                     │
   Controller → Service (business)                                                │
     • AuditService.record(...)        → audit_log table + AUDIT logger           │
     • SessionService.start/endSession → user_sessions table                      │
     • AuditLogService.record(...)     → SECURITY_AUDIT logger (auth events)       │
            │                                                                     │
   GlobalExceptionHandler → ErrorResponse{ …, correlationId }                     │
            └─────────────────────────────────────────────────────────────────────┘

Logback (logback-spring.xml) routes loggers → rotated, gzip'd files:
  root → mana-service.log + error.log (+ console)
  SECURITY_AUDIT → security.log     AUDIT → audit.log
  AUCTION → auction.log             CHAT → chat.log        NOTIFICATION → notification.log
  com.manacommunity.api(.service).scheduler → scheduler.log
```

Every log line carries `[cid=<correlationId> uid=<userId>]` from the MDC
(`-` when off-request, e.g. startup/schedulers).

---

## 2. Log levels

| Level | Local | Dev | Prod | Use for |
|-------|-------|-----|------|---------|
| TRACE | on | off | off | very fine-grained tracing |
| DEBUG | on | off | off | developer diagnostics |
| INFO  | on | on | on | business events, request log |
| WARN  | on | on | on | recoverable issues, slow APIs (≥2s) |
| ERROR | on | on | on | failures, unhandled exceptions, slow APIs (≥5s) |

Levels are controlled via `logging.level.*` in `application-*.yaml` (not in `logback-spring.xml`).

---

## 3. Correlation IDs

- Header: **`X-Correlation-Id`**. Generated if absent, sanitized (`[a-zA-Z0-9-]`, ≤64 chars) to prevent log forging.
- Available everywhere via `MDC.get("correlationId")` and on every log line.
- Returned on the response header **and** in every `ErrorResponse`, so a user can quote it to support.
- Frontend: read it from the response header / error body and show it on error screens.

---

## 4. Sensitive data — NEVER log

Never write these in clear text, anywhere (logs, audit `old/new` values, exceptions):

- Passwords, OTPs, PINs, CVV
- JWT / refresh / access tokens, API keys, secrets
- Aadhaar / government IDs, bank data
- Full email / mobile (mask them)

Use **`MaskingUtil`**:

```java
MaskingUtil.maskEmail("sandeep@gmail.com"); // sa*****@gmail.com
MaskingUtil.maskMobile("9876543210");        // 98******10
MaskingUtil.maskAadhaar("1234 5678 9012");   // ********9012
MaskingUtil.maskToken(jwt);                  // eyJh…***  (never the full token)
MaskingUtil.redact(password);                // ***
```

`RequestResponseLoggingFilter` already masks sensitive query params
(`password`, `otp`, `token`, `aadhaar`, `pin`, `cvv`, …). Add new keys to its
`SENSITIVE_PARAMS` set when introducing new sensitive parameters.

---

## 5. Audit logging (business events → DB)

Use **`AuditService`** for any sensitive mutation. It persists to `audit_log` and mirrors
to the `AUDIT` logger; it resolves actor/IP/correlation id automatically and **never throws**
into the business flow.

```java
auditService.record(
    AuditAction.USER_CREATED, AuditModule.USER_MANAGEMENT,
    "AppUser", String.valueOf(user.getId()),
    /* oldValue */ null,
    /* newValue */ "role=MEMBER, community=" + communityId);
```

Rules:
- Call it **after** the successful save (so failed operations aren't audited as done).
- Put **only safe, masked** values in `oldValue`/`newValue` — never secrets.
- `action`/`module` are stored as **strings** (not DB enums) on purpose, so renaming/removing
  an enum constant never breaks reads of historical rows. Add new constants to
  `AuditAction` / `AuditModule`.

Currently wired: `USER_CREATED`, `BID_PLACED`, `PLAYER_SOLD`, `TEAM_CREATED`,
`PERMISSION_CHANGED` (role + user). To extend: inject `AuditService` and add one `record(...)`
call at the mutation point. Remaining candidates: `MATCH_*`, `WINNER_DECLARED`,
`TOURNAMENT_COMPLETED`, `AUCTION_STARTED/ENDED`, marketplace `PRODUCT_*` (no backend module yet).

View it: **Admin → Audit Trail** (`/admin/audit-logs`, SUPER_ADMIN) →
`GET /api/admin/audit-logs` (+ `/stats`).

---

## 6. Security & session logging

- **Auth events** (login/logout/lockout/refresh/multi-device) → `AuditLogService` → `SECURITY_AUDIT`
  logger → `security.log`. Emails are masked.
- **Sessions** → `SessionService.startSession/endSession` → `user_sessions` table
  (device, browser, IP, login/logout time, status). Concurrent active sessions raise
  `MULTIPLE_DEVICE_LOGIN`.
- View it: the **Login Sessions** section of the Audit Trail page →
  `GET /api/admin/sessions` (+ `/stats`).

JWT is stateless, so `user_sessions` is an **observability trail**, not a server-side session
store — logout closes the audit record, not a live token (short access-token TTL bounds exposure).

---

## 7. Module loggers

Get a named logger so output lands in the right file:

```java
private static final Logger AUCTION = LoggerFactory.getLogger("AUCTION");        // → auction.log
private static final Logger CHAT = LoggerFactory.getLogger("CHAT");              // → chat.log  (NEVER message content)
private static final Logger NOTIFICATION = LoggerFactory.getLogger("NOTIFICATION"); // → notification.log
```

Scheduler classes log to `scheduler.log` automatically (package-based routing).
**Chat:** log conversation/message *metadata only* (ids, delivered/read, timestamps) — never message bodies.

---

## 8. Log files & rotation

Defined in `src/main/resources/logback-spring.xml`. Each file: daily + 10MB rolling,
30-day retention, gzip archives under `logs/archive/`, total size cap.

| File | Contents |
|------|----------|
| `mana-service.log` | primary app log (read by the `/admin/logs` dashboard) |
| `error.log` | ERROR level only |
| `security.log` | `SECURITY_AUDIT` (auth/security events) |
| `audit.log` | `AUDIT` (business audit mirror) |
| `scheduler.log` | tournament scheduler packages |
| `auction.log` / `chat.log` / `notification.log` | module loggers |

> The primary file path (`logs/mana-service.log`) must stay in sync with `logging.file.name`
> in `application.yaml` — `SystemLogController` reads it for the in-app log viewer.

---

## 9. Deployment checklist

- [ ] Apply DDL on prod **before** deploying (profile uses `ddl-auto: validate`):
      `db/sql/v1.0.0/12_audit_log.sql`, `db/sql/v1.0.0/13_user_sessions.sql`
- [ ] Ensure the runtime user can create/write `logs/` and `logs/archive/`.
- [ ] Confirm `logging.level.*` is INFO/WARN/ERROR in `application-prod.yaml`.
- [x] Actuator + Micrometer (Prometheus) wired — see §10. For prod scraping, decide the
      auth path (internal management port vs long-lived token) per `monitoring/README.md`.

---

## 10. Metrics & alarms (Actuator + Micrometer + Prometheus) — IMPLEMENTED

Actuator + Micrometer are wired with a **Prometheus** registry (free, self-hosted — no
paid metrics backend). Metrics are exposed at `/actuator/prometheus`.

**Endpoints & security** (`SecurityConfig`):
- `/actuator/health`, `/actuator/info` → **public** (load-balancer/EC2 probe). The `mail`
  health contributor is disabled so a down SMTP doesn't trip a false 503.
- `/actuator/prometheus`, `/actuator/metrics`, all others → **SUPER_ADMIN only**.

**Custom business metrics** (auto metrics like `http_server_requests`, `jvm_*`, `hikaricp_*`
come for free):

| Metric | Source | Tags |
|--------|--------|------|
| `chat_messages_sent_total` | `ChatService.sendMessage` | `type` (DIRECT/GROUP) |
| `auction_bids_placed_total` | `AuctionServiceImpl.placeBid` | `rtm` (true/false) |
| `admin_logs_fetch_errors_total` | `SystemLogServiceImpl.getLogTail` | `logType`, `exception` |

**Add a metric:** inject `io.micrometer.core.instrument.MeterRegistry` and, after the
successful business action, `meterRegistry.counter("name", "tag", value).increment();`
(or `Timer`/`Gauge`). Keep tag cardinality low — never tag with ids/emails.

**Stack to run it:** `monitoring/` (docker-compose: Prometheus + Grafana, datasource +
dashboard auto-provisioned, alert rules). The `AdminLogsServerError` alarm fires on any
5xx to `/api/admin/logs`. See `monitoring/README.md` for the prod scrape-auth options.

## 10b. Still on the roadmap

- **DB query monitoring** — enable `hibernate.generate_statistics` + a slow-query threshold
  (HikariCP pool metrics are already exported via Micrometer and on the dashboard).
- **Alertmanager wiring** — route the alert rules to Slack/email/PagerDuty (the rules exist;
  Alertmanager is commented out in `monitoring/prometheus/prometheus.yml`).
- **Distributed tracing across services** — current correlation id is per-service; for multi-service
  tracing adopt W3C `traceparent` / OpenTelemetry.
- **Remaining audit events** — scheduler/match/auction-status (§5).

---

## 11. Quick reference — do / don't

**Do**
- Use `AuditService` for sensitive mutations (after success).
- Use `MaskingUtil` for any PII/secret that must appear in a log.
- Use the named logger for module-specific logs.
- Rely on the MDC correlation id — don't pass ids around manually.

**Don't**
- Don't log passwords, OTPs, tokens, Aadhaar, bank data — ever.
- Don't log chat message content or request/response bodies.
- Don't store `action`/`module`/`status` as DB enums (use strings — avoids enum-drift breakage).
- Don't let auditing/logging throw into the business flow (the services already swallow + log failures).
