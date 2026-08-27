@e2e @pooja @smoke
Feature: Ganesh Mahotsav 2026 — End-to-end event module flow

  # Full lifecycle automated in one orchestrated scenario:
  #   1.  Admin seeds community (ensure inviteCode is valid)
  #   2.  Create 50 devotee accounts in parallel
  #   3.  Admin creates Ganesh Mahotsav event (Sept 14–19)
  #   4.  Admin creates 3 Pooja seva sub-events with correct slot patterns
  #   5.  Devotee logs in and registers for a pooja slot
  #   6.  Database state is fully verified

  Scenario: Full Ganesh Mahotsav event module lifecycle
    # ── 1. Seed community (no-op if already seeded) ─────────────────────
    Given url baseUrl + '/admin/seed/all'
    When method POST
    Then status 200
    * print '✅ Community seed complete'

    # ── 2. Create 50 devotee accounts ────────────────────────────────────
    * call read('classpath:karate/features/users/create-bulk-users.feature')
    * print '✅ 50 devotee accounts created'

    # ── 3. Create community event ─────────────────────────────────────────
    * def eventResult = call read('classpath:karate/features/events/create-event.feature')
    * def eventId     = eventResult.eventId
    * print '✅ Event created:', eventId

    # ── 4. Create pooja seva sub-events ──────────────────────────────────
    * set createdEventId = eventId
    * def poojaResult     = call read('classpath:karate/features/pooja/create-pooja-sevas.feature')
    * def seva14EveningId = poojaResult.seva14EveningId
    * def sevaMainDaysId  = poojaResult.sevaMainDaysId
    * def seva19MorningId = poojaResult.seva19MorningId
    * print '✅ Pooja sevas — Sept14 evening:', seva14EveningId, '| Main days:', sevaMainDaysId, '| Sept19 morning:', seva19MorningId

    # ── 5. Devotee registers for Sept 14 evening slot ────────────────────
    * set userEmail    = 'devotee1@ganesh2026.test'
    * set userPassword = 'Test@1234'
    * call read('classpath:karate/features/pooja/register-for-pooja.feature')
    * print '✅ Devotee1 pooja registration complete'

    # ── 6. Verify DB state ───────────────────────────────────────────────
    * set verifyEventId = eventId
    * call read('classpath:karate/features/db/verify-event-db.feature')

    * print ''
    * print '═══════════════════════════════════════════════════════════'
    * print '✅  GANESH MAHOTSAV 2026 — Full E2E Flow PASSED'
    * print '   Event ID         :', eventId
    * print '   Sept 14 Seva     :', seva14EveningId
    * print '   Main Days Seva   :', sevaMainDaysId
    * print '   Sept 19 Seva     :', seva19MorningId
    * print '═══════════════════════════════════════════════════════════'
