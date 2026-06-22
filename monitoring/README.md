# Monitoring stack (Prometheus + Grafana) — self-hosted, free

This folder runs a complete, free observability stack against the Mana Community
backend's Micrometer metrics (exposed at `/actuator/prometheus`). No paid vendor
backend — you only pay for the compute it runs on.

```
Spring Boot app                Prometheus                 Grafana
/actuator/prometheus  ──scrape──►  TSDB + alert rules  ──query──►  dashboards + alerts
   (Micrometer)                    (alerts.yml)                    (mana-overview.json)
```

## What's here
| Path | Purpose |
|------|---------|
| `docker-compose.yml` | Brings up Prometheus + Grafana |
| `prometheus/prometheus.yml` | Scrape config (auth + target) |
| `prometheus/alerts.yml` | Alert rules — incl. the `/api/admin/logs` 5xx alarm |
| `grafana/provisioning/` | Auto-wires the datasource + dashboard loader |
| `grafana/dashboards/mana-overview.json` | Starter dashboard |
| `.env.example` | Copy to `.env`; sets the app target/scheme/env |

## Quick start
```bash
cd monitoring
cp .env.example .env            # edit MANA_TARGET / MANA_SCHEME / MANA_ENV
echo "<SUPER_ADMIN_JWT>" > prometheus/token.txt   # see "Auth" below
docker compose up -d
```
- Grafana → http://localhost:3000 (`admin` / `GRAFANA_ADMIN_PASSWORD` from `.env`)
  → **Dashboards → Mana Community → Service Overview**.
- Prometheus → http://localhost:9090 → **Status → Targets** (app should be `UP`),
  **Alerts** shows rule state.

## Auth — important
`/actuator/prometheus` is **SUPER_ADMIN-only** (see `SecurityConfig`). The app's
JWTs expire in 15 min and Prometheus can't refresh them, so a normal login token
won't keep working. Two production-grade options (chosen in `prometheus.yml`):

- **(A) Internal management port — recommended.** Expose actuator on a separate port
  bound to the VPC/private network and scrape that over a trusted network, no token:
  ```yaml
  # application-prod.yaml
  management:
    server:
      port: 9000          # firewall this to the Prometheus host only
  ```
  Then set `MANA_TARGET=<host>:9000` and delete the `authorization:` block.

- **(B) Long-lived service token — simplest.** Put a long-TTL SUPER_ADMIN JWT in
  `prometheus/token.txt` (git-ignored). Standing credential → rotate it periodically.
  This is the default in `prometheus.yml`.

## The alarm you asked for
`AdminLogsServerError` (in `alerts.yml`) fires when **any 5xx hits `/api/admin/logs`**
— the endpoint that returned 500 on EC2. It reads the auto Micrometer metric
`http_server_requests_seconds_count{uri="/api/admin/logs", status=~"5.."}`. To actually
get paged, point Prometheus at an Alertmanager (Slack/email/PagerDuty) — uncomment the
`alerting:` block in `prometheus.yml`.

## Dashboard panels
App up • `/api/admin/logs` 5xx • overall 5xx rate • JVM heap % • request rate by status
• p95 latency by route • chat messages/min • auction bids/min • HikariCP pool.

## Metrics reference
| Metric | Source | Tags |
|--------|--------|------|
| `http_server_requests_seconds_*` | auto (Actuator) | `uri`, `status`, `method`, `outcome` |
| `jvm_memory_*`, `hikaricp_*`, `jvm_gc_*` | auto (Actuator) | — |
| `chat_messages_sent_total` | `ChatService.sendMessage` | `type` (DIRECT/GROUP) |
| `auction_bids_placed_total` | `AuctionServiceImpl.placeBid` | `rtm` (true/false) |
| `admin_logs_fetch_errors_total` | `SystemLogServiceImpl.getLogTail` | `logType`, `exception` |
