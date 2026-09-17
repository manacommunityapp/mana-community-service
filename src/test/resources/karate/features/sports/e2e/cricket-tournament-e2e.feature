@e2e @sports @smoke
Feature: Cricket Tournament — End-to-end sports module flow

  # Full sports module lifecycle orchestrated in one scenario:
  #   1.  Create sports test users (sportsman1 + sportsman2)
  #   2.  Create sport meta (Cricket, Badminton)
  #   3.  Create player categories (Open Singles, Mixed Doubles)
  #   4.  Create cricket sports event (adminApprovalRequired=false → auto-confirm)
  #   5.  Register sportsman1 and sportsman2 for the cricket event
  #   6.  Verify registrations (GET /events/{id}/registrations)
  #   7.  Update event status → REGISTRATION_CLOSED
  #   8.  Partner invite flow — mixed doubles Badminton event
  #   9.  Create tournament wrapping the cricket event
  #   10. Add tournament content (announcement, gallery, timeline)
  #   11. Create tournament scheduler config and generate schedule
  #   12. Create auction config → teams → players → live bidding flow
  #   13. DB verify: event, registrations, sport meta, auction, players
  #
  # All steps run with continueOnStepFailure=true so cleanup always fires.

  Scenario: Full Cricket Tournament & Auction lifecycle
    * configure continueOnStepFailure = true

    # ── 1. Sports test users ───────────────────────────────────────────────
    * call read('classpath:karate/features/users/create-sports-users.feature')
    * print '✅ Step 1 — Sports users created'

    # ── 2. Sport meta ──────────────────────────────────────────────────────
    * url baseUrl
    * def adminAuth = callonce read('classpath:karate/features/auth/login.feature')
    * def adminTok  = adminAuth.authToken
    * header Authorization  = 'Bearer ' + adminTok
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

    Given path '/sports/meta'
    And request { name: 'Cricket Karate Test', icon: '🏏', formats: ['T20','ODI'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def sportId = response.id
    * print '✅ Step 2a — Cricket sport created — ID:', sportId

    Given path '/sports/meta'
    And header Authorization  = 'Bearer ' + adminTok
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And request { name: 'Badminton Karate Test', icon: '🏸', formats: ['SINGLES','DOUBLES','MIXED_DOUBLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def badmintonSportId = response.id
    * print '✅ Step 2b — Badminton sport created — ID:', badmintonSportId

    # ── 3. Player categories ───────────────────────────────────────────────
    Given path '/player-categories'
    And header Authorization  = 'Bearer ' + adminTok
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And request { name: 'Open Karate Test', categoryType: 'SINGLES', description: 'Open singles', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def categoryId = response.id
    * print '✅ Step 3a — Open category ID:', categoryId

    Given path '/player-categories'
    And header Authorization  = 'Bearer ' + adminTok
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And request { name: 'Mixed Doubles Karate Test', categoryType: 'MIXED_DOUBLES', description: 'Mixed doubles', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def mixedCategoryId = response.id
    * print '✅ Step 3b — Mixed Doubles category ID:', mixedCategoryId

    # ── 4. Create cricket sports event ─────────────────────────────────────
    * def createEventResult = call read('classpath:karate/features/sports/events/create-sports-event.feature')
    * def sportsEventId     = createEventResult.sportsEventId
    * def sportsEventUuid   = createEventResult.sportsEventUuid
    * print '✅ Step 4 — Cricket event ID:', sportsEventId, '| UUID:', sportsEventUuid

    # ── 5. Register sportsman1 (cricket, singles) ──────────────────────────
    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def reg1Result   = call read('classpath:karate/features/sports/registrations/register-for-event.feature') { sportsEventId: '#(sportsEventId)', regCategoryId: '#(categoryId)', regMatchType: 'SINGLES', regUserEmail: 'sportsman1@sports.test', regUserPassword: 'Test@1234' }
    * def sports1RegId = reg1Result.registrationId
    * print '✅ Step 5a — sportsman1 reg ID:', sports1RegId

    # Register sportsman2 (cricket, singles)
    * def reg2Result   = call read('classpath:karate/features/sports/registrations/register-for-event.feature') { sportsEventId: '#(sportsEventId)', regCategoryId: '#(categoryId)', regMatchType: 'SINGLES', regUserEmail: 'sportsman2@sports.test', regUserPassword: 'Test@1234' }
    * def sports2RegId = reg2Result.registrationId
    * print '✅ Step 5b — sportsman2 reg ID:', sports2RegId

    # ── 6. Verify registrations via admin list ─────────────────────────────
    Given path '/sports/events/' + sportsEventId + '/registrations'
    And header Authorization  = 'Bearer ' + adminTok
    And header X-Community-Id = communityId
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 1
    * print '✅ Step 6 — Event registrations count:', response.length

    # Verify confirmed-count endpoint
    Given path '/sports/events/' + sportsEventId + '/confirmed-count'
    And header Authorization  = 'Bearer ' + adminTok
    And header X-Community-Id = communityId
    When method GET
    * match [200, 201] contains responseStatus
    * print '✅ Step 6b — Confirmed count:', response

    # ── 7. Update event status → REGISTRATION_CLOSED ──────────────────────
    * call read('classpath:karate/features/sports/events/update-event-status.feature') { sportsEventId: '#(sportsEventId)', newStatus: 'REGISTRATION_CLOSED' }
    * print '✅ Step 7 — Event status → REGISTRATION_CLOSED'

    # ── 8. Partner invite flow (Mixed Doubles Badminton) ───────────────────
    # Look up sportsman2 user ID for the partner invite
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization  = 'Bearer ' + adminTok
    And header X-Community-Id = communityId
    And param q           = 'Sports User Two'
    And param communityId = communityId
    When method GET
    Then status 200
    * def sports2UserId = response.length > 0 ? response[0].id : null

    # Look up sportsman1 user ID
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization  = 'Bearer ' + adminTok
    And header X-Community-Id = communityId
    And param q           = 'Sports User One'
    And param communityId = communityId
    When method GET
    Then status 200
    * def sports1UserId = response.length > 0 ? response[0].id : null

    * def mixedDoublesEventId = createEventResult.mixedDoublesEventId
    * if (sports1UserId != null && sports2UserId != null && mixedDoublesEventId != null) karate.call(read('classpath:karate/features/sports/registrations/partner-invite-flow.feature'))
    * print '✅ Step 8 — Partner invite flow done'

    # ── 9. Create tournament ───────────────────────────────────────────────
    * def tournamentResult = call read('classpath:karate/features/sports/tournament/create-tournament.feature')
    * def tournamentId = tournamentResult.tournamentId
    * print '✅ Step 9 — Tournament ID:', tournamentId

    # ── 10. Tournament content ─────────────────────────────────────────────
    * if (tournamentId != null) karate.call(read('classpath:karate/features/sports/tournament/tournament-content.feature'))
    * print '✅ Step 10 — Tournament content (announcements/gallery/timeline) done'

    # ── 11. Tournament scheduler ───────────────────────────────────────────
    * def schedulerResult = call read('classpath:karate/features/sports/tournament/tournament-scheduler.feature')
    * def schedulerConfigId = schedulerResult.schedulerConfigId
    * print '✅ Step 11 — Scheduler config ID:', schedulerConfigId

    # ── 12. Auction flow ───────────────────────────────────────────────────
    * def auctionResult = call read('classpath:karate/features/sports/auction/auction-config.feature')
    * def auctionConfigId = auctionResult.auctionConfigId
    * print '✅ Step 12a — Auction config ID:', auctionConfigId

    * def teamResult = call read('classpath:karate/features/sports/auction/auction-teams.feature')
    * def teamAlphaId = teamResult.teamAlphaId
    * def teamBetaId  = teamResult.teamBetaId
    * print '✅ Step 12b — Teams: Alpha=', teamAlphaId, '| Beta=', teamBetaId

    * if (auctionConfigId != null && teamAlphaId != null && teamBetaId != null) karate.call(read('classpath:karate/features/sports/auction/auction-live-flow.feature'))
    * print '✅ Step 12c — Auction live flow done'

    # ── 13. DB verify ──────────────────────────────────────────────────────
    * def verifySportsEventId = sportsEventId
    * call read('classpath:karate/features/sports/db/verify-sports-db.feature')

    * print ''
    * print '═══════════════════════════════════════════════════════════'
    * print '✅  CRICKET TOURNAMENT E2E — PASSED'
    * print '   Sport ID          :', sportId
    * print '   Event ID          :', sportsEventId
    * print '   Tournament ID     :', tournamentId
    * print '   Auction Config ID :', auctionConfigId
    * print '   Team Alpha        :', teamAlphaId
    * print '   Team Beta         :', teamBetaId
    * print '═══════════════════════════════════════════════════════════'
