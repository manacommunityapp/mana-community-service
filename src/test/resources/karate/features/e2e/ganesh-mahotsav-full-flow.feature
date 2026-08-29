@e2e @pooja @smoke
Feature: Ganesh Mahotsav 2026 — End-to-end event module flow

  # Full lifecycle orchestrated in one scenario:
  #   1.  Admin authenticates
  #   2.  Create devotee accounts (bulk)
  #   3.  Admin creates Ganesh Mahotsav event (Sept 14–19)
  #   4.  Update event (capacity, notes)
  #   5.  Admin creates 3 Pooja seva sub-events
  #   6.  Available-dates & schedule detail checks
  #   7.  Admin schedule management (create, status-patch, reservations view)
  #   8.  Devotee1 self-registers for Sept 14 evening slot
  #   9.  Admin registers devotee2 via admin-create endpoint
  #   10. Admin pooja registration management (list, summary, participants)
  #   11. General event registration (register, confirm, check-in, unregister)
  #   12. Reschedule + cancel a throwaway registration
  #   13. Database state is verified

  Scenario: Full Ganesh Mahotsav event module lifecycle
    # Keep going through all steps even if one sub-feature fails —
    # @AfterAll cleanup must always run on real data, not partial state.
    * configure continueOnStepFailure = true

    # ── 1. Authenticate as admin ──────────────────────────────────────────
    * url baseUrl
    * header Content-Type = 'application/json'
    * def loginResp = call read('classpath:karate/features/auth/login.feature')
    * def adminToken = loginResp.authToken
    * header Authorization  = 'Bearer ' + adminToken
    * header X-Community-Id = communityId
    * print '✅ Step 1 — Admin authenticated'

    # ── 2. Create devotee accounts ────────────────────────────────────────
    * call read('classpath:karate/features/users/create-bulk-users.feature')
    * print '✅ Step 2 — Devotee accounts created'

    # ── 3. Create community event ──────────────────────────────────────────
    * def eventResult = call read('classpath:karate/features/events/create-event.feature')
    * def eventId     = eventResult.eventId
    * print '✅ Step 3 — Event created:', eventId

    # ── 4. Update event (capacity + notes) ────────────────────────────────
    * call read('classpath:karate/features/events/update-event.feature')
    * print '✅ Step 4 — Event updated'

    # ── 5. Create pooja seva sub-events ───────────────────────────────────
    * def createdEventId  = eventId
    * def poojaResult     = call read('classpath:karate/features/pooja/create-pooja-sevas.feature')
    * def seva14EveningId = poojaResult.seva14EveningId
    * def sevaMainDaysId  = poojaResult.sevaMainDaysId
    * def seva19MorningId = poojaResult.seva19MorningId
    * print '✅ Step 5 — Sevas — Sept14 evening:', seva14EveningId, '| Main days:', sevaMainDaysId, '| Sept19 morning:', seva19MorningId

    # ── 6. Available dates & schedule detail ──────────────────────────────
    * call read('classpath:karate/features/pooja/available-dates.feature')
    * print '✅ Step 6 — Available dates & single-schedule checks passed'

    # ── 7. Admin schedule management ──────────────────────────────────────
    * call read('classpath:karate/features/pooja/schedule-management.feature')
    * print '✅ Step 7 — Schedule management (create, block, re-open, reservations list) passed'

    # ── 8. Devotee self-registers for Sept 14 evening slot ────────────────
    * def userEmail    = 'devotee1@ganesh2026.test'
    * def userPassword = 'Test@1234'
    * call read('classpath:karate/features/pooja/register-for-pooja.feature')
    * print '✅ Step 8 — Devotee1 pooja registration complete'

    # ── 9. Admin registers devotee2 on their behalf ───────────────────────
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization  = 'Bearer ' + adminToken
    And header X-Community-Id = communityId
    And param q           = 'devotee2'
    And param communityId = communityId
    When method GET
    Then status 200
    * def targetDevoteeUserId = response[0].id
    * call read('classpath:karate/features/pooja/admin-register-for-pooja.feature')
    * print '✅ Step 9 — Admin registration for devotee2 complete'

    # ── 10. Admin pooja registration management ───────────────────────────
    * call read('classpath:karate/features/pooja/pooja-admin-manage.feature')
    * print '✅ Step 10 — Admin list, summary, participants, single-registration checks passed'

    # ── 11. General event registration flow ───────────────────────────────
    * call read('classpath:karate/features/events/event-registration.feature')
    * print '✅ Step 11 — General event registration (register/confirm/check-in/unregister) passed'

    # ── 12. Reschedule + cancel a throwaway registration ──────────────────
    * call read('classpath:karate/features/pooja/pooja-reschedule.feature')
    * print '✅ Step 12 — Reschedule & cancel flow passed'

    # ── 13. Verify DB state ───────────────────────────────────────────────
    * def verifyEventId = eventId
    * call read('classpath:karate/features/db/verify-event-db.feature')

    * print ''
    * print '═══════════════════════════════════════════════════════════'
    * print '✅  GANESH MAHOTSAV 2026 — Full E2E Flow PASSED'
    * print '   Event ID            :', eventId
    * print '   Sept 14 Seva        :', seva14EveningId
    * print '   Main Days Seva      :', sevaMainDaysId
    * print '   Sept 19 Seva        :', seva19MorningId
    * print '   Devotee2 User ID    :', targetDevoteeUserId
    * print '═══════════════════════════════════════════════════════════'
