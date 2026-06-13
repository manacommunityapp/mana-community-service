# Schema De-duplication — Staged Migration Plan

Consolidates the three remaining duplicate models onto their richer "sports_*" counterparts.
Each is staged so it can be shipped and verified independently, with backfill + rollback.

**Conventions**
- DB schema: `manacommunity`. Prod runs `ddl-auto: validate` (Hibernate never drops/creates tables),
  so **physical table drops are a deliberate, final, separate step** — code removal alone leaves the
  old table as a harmless orphan.
- Backfill SQL is written **idempotent** (`WHERE NOT EXISTS …`) so it can live in
  `SchemaConstraintPatcher` (runs every startup, PostgreSQL-only, already H2-guarded) **or** be run
  once by hand. Recommendation: patcher for self-healing across environments.
- Already done (prerequisite, shipped): removed dead `AuctionEvent`/`EventRegistration` + repos.

Suggested order: **Sponsors → Notifications → Users** (ascending entanglement; each de-risks the next).

---

## 1. `event_sponsor` → `sports_event_sponsor`  ✅ DONE

**Current**
- `EventSponsor` (`event_sponsor`): `id, event_id, category, name, url, created_at`.
- `SportsEventSponsor` (`sports_event_sponsor`): same + `tournament_id` (superset).
- Wiring: `SportsEvent.sponsors : List<EventSponsor>` ([SportsEvent.java:112](../src/main/java/com/manacommunity/api/model/SportsEvent.java#L112));
  built in `SportsEventServiceImpl` at [~90](../src/main/java/com/manacommunity/api/service/impl/SportsEventServiceImpl.java#L90)
  and [~566](../src/main/java/com/manacommunity/api/service/impl/SportsEventServiceImpl.java#L566).

**Phase A — repoint code (no data yet)**
1. `SportsEvent.sponsors` → `List<SportsEventSponsor>` (keep `mappedBy="event"`, cascade/orphan as-is).
2. The two `EventSponsor.builder()` sites → `SportsEventSponsor.builder()` (same fields; leave `tournament` null).
3. Any response/DTO mapping that reads `getSponsors()` keeps working (same `name/category/url` getters).

**Phase A — backfill existing rows** (idempotent):
```sql
INSERT INTO manacommunity.sports_event_sponsor (event_id, tournament_id, category, name, url, created_at)
SELECT es.event_id, NULL, es.category, es.name, es.url, es.created_at
FROM manacommunity.event_sponsor es
WHERE NOT EXISTS (
  SELECT 1 FROM manacommunity.sports_event_sponsor s
  WHERE s.event_id = es.event_id AND s.name = es.name AND s.category = es.category);
```

**Phase B — retire**: delete `EventSponsor.java` + `EventSponsorRepository` (if it exists). Compile.

**Verify**: create an event with sponsors via the API → row lands in `sports_event_sponsor`; existing
events still list their sponsors. **Rollback**: revert the commit; `event_sponsor` rows are untouched
(backfill only ever inserts into the new table).

---

## 2. `event_notification_schedule` → `sports_notification_scheduler`  ✅ DONE
> Frontend follow-up: event JSON now exposes only `premiumNotifications` (the legacy `notifications`
> array was removed) — update any UI that read `event.notifications`.


**Current**
- `EventNotificationSchedule` (`event_notification_schedule`): `id, event_id, notify_at, type, title, body, sent, created_at`.
- `SportsNotificationScheduler` (`sports_notification_scheduler`): rich superset — `trigger_key, label,
  offset_minutes, enabled, recipients, channels, priority, is_custom, notify_at, updated_at, tournament_id`.
- Wiring:
  - `NotificationScheduler` runs **two loops** every 60s — legacy `notifRepo.findByNotifyAtBeforeAndSentFalse(...)`
    and `premiumRepo.findByNotifyAtBeforeAndSentFalseAndEnabledTrue(...)` ([NotificationScheduler.java:50,70](../src/main/java/com/manacommunity/api/scheduler/NotificationScheduler.java#L50)).
  - `SportsEvent.notifications : List<EventNotificationSchedule>` ([:104](../src/main/java/com/manacommunity/api/model/SportsEvent.java#L104)).
  - Built in `SportsEventServiceImpl` at [~373](../src/main/java/com/manacommunity/api/service/impl/SportsEventServiceImpl.java#L373).

**Field mapping** (legacy → rich):
`type → trigger_key` and `label`; `offset_minutes = 0` (legacy already stores absolute `notify_at`);
`enabled = true`; `recipients = 'CONFIRMED'`; `channels = 'push'`; `priority = 'NORMAL'`;
`is_custom = true`; carry `title, body, sent, notify_at, created_at`.

**Phase A — repoint code**
1. `SportsEvent.notifications` → `List<SportsNotificationScheduler>`.
2. The `EventNotificationSchedule.builder()` site → `SportsNotificationScheduler.builder()` filling the
   defaults above.
3. **Remove the legacy loop** from `NotificationScheduler` (drop `notifRepo` + its block); keep only the
   `premiumRepo` loop. (The rich scheduler already covers the legacy use case.)

**Phase A — backfill** (idempotent):
```sql
INSERT INTO manacommunity.sports_notification_scheduler
  (event_id, tournament_id, trigger_key, label, offset_minutes, enabled, title, body,
   recipients, channels, priority, is_custom, sent, notify_at, created_at)
SELECT e.event_id, NULL, e.type, e.type, 0, true,
       COALESCE(e.title,''), COALESCE(e.body,''),
       'CONFIRMED', 'push', 'NORMAL', true, e.sent, e.notify_at, e.created_at
FROM manacommunity.event_notification_schedule e
WHERE NOT EXISTS (
  SELECT 1 FROM manacommunity.sports_notification_scheduler s
  WHERE s.event_id = e.event_id AND s.notify_at = e.notify_at AND s.title = COALESCE(e.title,''));
```
> Migrate **only `sent = false`** rows if you don't want historical/sent reminders re-evaluated
> (add `AND e.sent = false`). Recommended to avoid any chance of re-sending.

**Phase B — retire**: delete `EventNotificationSchedule.java` + `EventNotificationScheduleRepository`.

**Verify**: schedule a near-future notification via the API → it persists in
`sports_notification_scheduler` and fires once through the single remaining loop; no double-sends.
**Rollback**: revert commit; legacy rows untouched. (Risk to watch: the *behavioral* change of dropping
the legacy loop — confirm nothing else writes `event_notification_schedule` after Phase A.)

---

## 3. `users` → `app_user`  ✅ DONE  (smaller than it first looked)

**Current** — the legacy footprint is tiny:
- `User` (`users`, **String PK**): `id, cognitoSub, fullName, email, phoneNumber, aadharNumberMasked,
  kycStatus, profileImageUrl, passwordHash, role, createdAt`.
- `AppUser` (`app_user`, **Long PK, IDENTITY**) — the live entity; auth uses it + the `role` String +
  direct `RolePermission` queries.
- Legacy references (only **three**):
  1. `UserRepository` (the legacy repo) — injected **only** in `UserSeeder` as `legacyUserRepo`.
  2. `UserSeeder` — seeds AppUser via `AppUserRepository`; also touches `legacyUserRepo`.
  3. `Role.users : Set<User>` back-reference ([Role.java:43](../src/main/java/com/manacommunity/api/model/Role.java#L43)).
- **No controller/service/security code reads the `users` table** (the earlier "~12 injections" was a
  grep artifact — those files use `AppUserRepository`).

**Phase A — sever the references**
1. Remove `Role.users` (`@OneToMany(mappedBy="role") Set<User>`) — verify no one calls `role.getUsers()`
   (grep first; expected: none).
2. In `UserSeeder`, delete the `legacyUserRepo` field and any legacy-user seeding; keep AppUser seeding.

**Phase A — optional backfill** (only if any `users` rows must survive as `app_user`; the table is
seed-only today, so usually a no-op):
```sql
INSERT INTO manacommunity.app_user (email, full_name, phone, password_hash, role, kyc_status, is_active)
SELECT u.email, u.full_name, u.phone_number, u.password_hash, 'MEMBER', u.kyc_status, true
FROM manacommunity.users u
WHERE u.email IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM manacommunity.app_user a WHERE a.email = u.email);
```
(Adjust column names to the real `app_user` schema; this is the only place the **String→bigint PK**
"mismatch" matters — we don't carry the String `id`, we let `app_user.id` IDENTITY assign a new Long.)

**Phase B — retire**: delete `User.java` + `UserRepository.java`. Compile.

**Related loose-ID cleanup (optional, same theme)**: `UserCommunityMapping.userId/communityId` are
`String` loose IDs. Once `users` is gone, either (a) convert them to `Long` + real `@ManyToOne` to
`app_user`/`community` with a backfill cast, or (b) leave as-is. Treat as a **separate** stage — it's the
only true column-type change and needs its own backfill (`ALTER … USING userid::bigint` after verifying
all values are numeric).

**Verify**: app boots, login/registration unaffected (they never used `users`), seeders run clean.
**Rollback**: revert commit; `users` table untouched.

---

## Final, deliberate step (all three) — drop the orphan tables
Only after the above ship and bake, and you've confirmed the orphan tables hold nothing you need:
```sql
DROP TABLE IF EXISTS manacommunity.event_sponsor;               -- consolidated → sports_event_sponsor
DROP TABLE IF EXISTS manacommunity.event_notification_schedule; -- consolidated → sports_notification_scheduler
DROP TABLE IF EXISTS manacommunity.users;                       -- consolidated → app_user
DROP TABLE IF EXISTS manacommunity.auction_event;               -- entity removed (dead)
DROP TABLE IF EXISTS manacommunity.event_registration;          -- entity removed (dead)
DROP TABLE IF EXISTS manacommunity.user_community_mapping;       -- entity removed (dead)
```
Run as a one-time reviewed migration (NOT in the auto-startup patcher — drops should never be automatic).
**Pre-drop checks:** the startup backfills have run (sponsors copied; pending `sent=false`
notifications copied) and the new tables look correct.

## Execution checklist
- [x] Stage 1 Sponsors: repoint code → backfill → delete `EventSponsor` → verify  ✅
- [x] Stage 2 Notifications: repoint code + drop legacy loop → backfill (`sent=false`) → delete legacy → verify  ✅
- [x] Stage 3 Users: sever `Role.users` + `UserSeeder` → delete `User`/`UserRepository` → verify  ✅
- [x] `UserCommunityMapping` removed as dead code (no String→Long conversion needed)  ✅
- [ ] **Frontend**: switch any `event.notifications` reads to `event.premiumNotifications` (Stage 2)
- [ ] **Final**: reviewed one-time `DROP TABLE` of the six orphan tables (above)
