@e2e @pooja @smoke
Feature: Ganesh Mahotsav 2026 — End-to-end event module flow

  # Full lifecycle orchestrated in one scenario:
  #   1.  Admin authenticates
  #   2.  Create devotee accounts (bulk)
  #   3.  Admin creates Ganesh Mahotsav event (Sept 14–19)
  #   4.  Admin creates 3 Pooja seva sub-events
  #   5.  Devotee1 self-registers for Sept 14 evening slot
  #   6.  Admin registers devotee2 via admin-create endpoint
  #   7.  Database state is verified

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
    * print '✅ Admin authenticated'

    # ── 2. Create devotee accounts ────────────────────────────────────────
    * call read('classpath:karate/features/users/create-bulk-users.feature')
    * print '✅ Devotee accounts created'

    # ── 3. Create community event ──────────────────────────────────────────
    * def eventResult = call read('classpath:karate/features/events/create-event.feature')
    * def eventId     = eventResult.eventId
    * print '✅ Event created:', eventId

    # ── 4. Create pooja seva sub-events ───────────────────────────────────
    * def createdEventId  = eventId
    * def poojaResult     = call read('classpath:karate/features/pooja/create-pooja-sevas.feature')
    * def seva14EveningId = poojaResult.seva14EveningId
    * def sevaMainDaysId  = poojaResult.sevaMainDaysId
    * def seva19MorningId = poojaResult.seva19MorningId
    * print '✅ Pooja sevas — Sept14 evening:', seva14EveningId, '| Main days:', sevaMainDaysId, '| Sept19 morning:', seva19MorningId

    # ── 5. Devotee self-registers for Sept 14 evening slot ────────────────
    * def userEmail    = 'devotee1@ganesh2026.test'
    * def userPassword = 'Test@1234'
    * call read('classpath:karate/features/pooja/register-for-pooja.feature')
    * print '✅ Devotee1 pooja registration complete'

    # ── 6. Admin registers devotee2 on their behalf ───────────────────────
    # Look up devotee2's user ID via admin user-search
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization  = 'Bearer ' + adminToken
    And header X-Community-Id = communityId
    And param q           = 'devotee2'
    And param communityId = communityId
    When method GET
    Then status 200
    * def targetDevoteeUserId = response[0].id
    * call read('classpath:karate/features/pooja/admin-register-for-pooja.feature')
    * print '✅ Admin registration for devotee2 complete'

    # ── 7. Verify DB state ────────────────────────────────────────────────
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
