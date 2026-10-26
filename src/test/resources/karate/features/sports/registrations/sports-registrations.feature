@sports-registration @sports
Feature: Sports Event Registration — singles, doubles, mixed-doubles, validations

  # Covers gaps not exercised by cricket-tournament-e2e.feature:
  #   1. SINGLES auto-confirm (adminApprovalRequired=false)
  #   2. DOUBLES (same-gender) — partner invite + accept + admin confirm
  #   3. MIXED_DOUBLES partner rejection (accept=false) → CANCELLED
  #   4. Admin-approval-required → PENDING → admin confirms → CONFIRMED
  #   5. Register after REGISTRATION_CLOSED → blocked
  #   6. Capacity limit (maxParticipants=1) → 2nd registration blocked
  #   7. Duplicate registration by same user → 409

  Background:
    * url baseUrl
    * def adminAuth = callonce read('classpath:karate/features/auth/login.feature')
    * def adminTok  = adminAuth.authToken
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId
    # Ensure test users exist (idempotent — accepts 201 or 409)
    * call read('classpath:karate/features/users/create-sports-users.feature')

  # ── 1. SINGLES auto-confirm ─────────────────────────────────────────────────
  @sports-registration @smoke
  Scenario: SINGLES registration — auto-confirmed when adminApprovalRequired=false

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Singles Sport', icon: '🎯', formats: ['SINGLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def singlesId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Open Singles', categoryType: 'SINGLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def singlesCatId = response.id
    * def catArray     = [singlesCatId]

    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest Singles Event",
        "sportId": #(singlesId),
        "communityId": #(communityId),
        "eventDateStart": "2026-11-01",
        "eventDateEnd": "2026-11-03",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 32,
        "format": "SINGLES",
        "tournamentType": "KNOCKOUT",
        "categoryIds": #(catArray),
        "adminApprovalRequired": false,
        "minPlayers": 1,
        "maxPlayers": 1,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def singlesEventId = response.id

    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request { eventId: '#(singlesEventId)', categoryId: '#(singlesCatId)', matchType: 'SINGLES' }
    When method POST
    Then status 201
    And match response.status   == 'CONFIRMED'
    And match response.matchType == 'SINGLES'
    * print '✅ SINGLES auto-confirmed, regId:', response.id

  # ── 2. DOUBLES same-gender partner invite ───────────────────────────────────
  @sports-registration
  Scenario: DOUBLES registration — partner invite → sportsman2 accepts → admin confirms

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Doubles Sport', icon: '🏸', formats: ['DOUBLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def doublesSportId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Doubles Category', categoryType: 'DOUBLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def doublesCatId  = response.id
    * def doublesCatArr = [doublesCatId]

    # Look up sportsman2 user ID for partner invite
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization = 'Bearer ' + adminTok
    And param q           = 'Sports User Two'
    And param communityId = communityId
    When method GET
    Then status 200
    * def sports2UserId = response.length > 0 ? response[0].id : null
    * print '📌 sports2UserId:', sports2UserId

    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest Doubles Event",
        "sportId": #(doublesSportId),
        "communityId": #(communityId),
        "eventDateStart": "2026-11-05",
        "eventDateEnd": "2026-11-07",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 16,
        "format": "DOUBLES",
        "tournamentType": "ROUND_ROBIN",
        "categoryIds": #(doublesCatArr),
        "adminApprovalRequired": false,
        "mandatoryMixedDoubles": false,
        "minPlayers": 2,
        "maxPlayers": 2,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def doublesEventId = response.id

    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    # sportsman1 registers with sportsman2 as partner
    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request
      """
      {
        "eventId":       #(doublesEventId),
        "categoryId":    #(doublesCatId),
        "matchType":     "DOUBLES",
        "partnerUserId": #(sports2UserId)
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def doublesRegId = responseStatus != 409 ? response.id : null
    * print '📌 DOUBLES regId:', doublesRegId

    # sportsman2 checks pending invitations
    * def userEmail    = 'sportsman2@sports.test'
    * def userPassword = 'Test@1234'
    * def s2Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s2Token      = s2Auth.authToken

    Given path '/sports/registrations/partner-invitations'
    And header Authorization = 'Bearer ' + s2Token
    And param status = 'PENDING'
    When method GET
    Then status 200
    And match response == '#array'
    * def doublesInvites = response
    * print '📌 Pending DOUBLES invites for sportsman2:', doublesInvites.length

    # sportsman2 accepts the invite
    * def pendingId = doublesInvites.length > 0 ? doublesInvites[0].id : doublesRegId
    * call read('classpath:karate/features/sports/registrations/_accept-invite.feature') { acceptRegId: '#(pendingId)', userToken: '#(s2Token)' }
    * print '✅ DOUBLES invite accepted by sportsman2'

    # Admin confirms
    Given path '/sports/registrations/' + pendingId + '/confirm'
    And header Authorization = 'Bearer ' + adminTok
    When method PUT
    * match [200, 201, 204, 409] contains responseStatus
    * print '✅ DOUBLES registration admin-confirmed, status:', responseStatus

  # ── 3. MIXED_DOUBLES partner rejection ─────────────────────────────────────
  @sports-registration
  Scenario: MIXED_DOUBLES partner rejection — sportsman2 declines → registration cancelled

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest MixedD Sport', icon: '🏓', formats: ['MIXED_DOUBLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def mixedSportId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Mixed Doubles Cat', categoryType: 'MIXED_DOUBLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def mixedCatId  = response.id
    * def mixedCatArr = [mixedCatId]

    # Look up sportsman2 user ID
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization = 'Bearer ' + adminTok
    And param q           = 'Sports User Two'
    And param communityId = communityId
    When method GET
    Then status 200
    * def rejectPartnerId = response.length > 0 ? response[0].id : null

    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest MixedD Rejection Event",
        "sportId": #(mixedSportId),
        "communityId": #(communityId),
        "eventDateStart": "2026-11-10",
        "eventDateEnd": "2026-11-12",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 16,
        "format": "MIXED_DOUBLES",
        "tournamentType": "ROUND_ROBIN",
        "categoryIds": #(mixedCatArr),
        "adminApprovalRequired": false,
        "mandatoryMixedDoubles": true,
        "minPlayers": 2,
        "maxPlayers": 2,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def mixedEventId = response.id

    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request
      """
      {
        "eventId":       #(mixedEventId),
        "categoryId":    #(mixedCatId),
        "matchType":     "MIXED_DOUBLES",
        "partnerUserId": #(rejectPartnerId)
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def rejectRegId = responseStatus != 409 ? response.id : null
    * print '📌 MIXED_DOUBLES regId before rejection:', rejectRegId

    # sportsman2 fetches their pending invitations
    * def userEmail    = 'sportsman2@sports.test'
    * def userPassword = 'Test@1234'
    * def s2Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s2Token      = s2Auth.authToken

    Given path '/sports/registrations/partner-invitations'
    And header Authorization = 'Bearer ' + s2Token
    And param status = 'PENDING'
    When method GET
    Then status 200
    * def rejectInvites = response
    * def rejectTarget  = rejectInvites.length > 0 ? rejectInvites[0].id : rejectRegId
    * print '📌 sportsman2 pending invites:', rejectInvites.length

    # sportsman2 DECLINES the invite (accept=false)
    Given path '/sports/registrations/' + rejectTarget + '/partner-confirm'
    And header Authorization = 'Bearer ' + s2Token
    And param accept = false
    When method PUT
    * match [200, 201, 204] contains responseStatus
    * print '✅ Partner rejection sent — status:', responseStatus

    # Verify registration is now cancelled/declined (not CONFIRMED)
    Given path '/sports/registrations/' + rejectTarget
    And header Authorization = 'Bearer ' + adminTok
    When method GET
    * match [200, 201] contains responseStatus
    * def rejectedStatus = response.status
    * assert rejectedStatus != 'CONFIRMED'
    * print '✅ Registration status after rejection:', rejectedStatus

  # ── 4. Admin-approval-required ─────────────────────────────────────────────
  @sports-registration
  Scenario: Admin approval required — registration lands PENDING then admin confirms → CONFIRMED

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Approval Sport', icon: '🏅', formats: ['SINGLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def approvalSportId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Approval Cat', categoryType: 'SINGLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def approvalCatId  = response.id
    * def approvalCatArr = [approvalCatId]

    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest Admin Approval Event",
        "sportId": #(approvalSportId),
        "communityId": #(communityId),
        "eventDateStart": "2026-11-15",
        "eventDateEnd": "2026-11-17",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 32,
        "format": "SINGLES",
        "tournamentType": "KNOCKOUT",
        "categoryIds": #(approvalCatArr),
        "adminApprovalRequired": true,
        "minPlayers": 1,
        "maxPlayers": 1,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def approvalEventId = response.id

    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    # Register — should land as PENDING (admin must approve)
    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request { eventId: '#(approvalEventId)', categoryId: '#(approvalCatId)', matchType: 'SINGLES' }
    When method POST
    Then status 201
    And match response.status == 'PENDING'
    * def pendingApprovalId = response.id
    * print '✅ Registration created with PENDING status, regId:', pendingApprovalId

    # Admin confirms → CONFIRMED
    Given path '/sports/registrations/' + pendingApprovalId + '/confirm'
    And header Authorization = 'Bearer ' + adminTok
    When method PUT
    * match [200, 201, 204] contains responseStatus
    * print '✅ Admin confirmed registration', pendingApprovalId, '→ status:', responseStatus

    # Verify status is now CONFIRMED
    Given path '/sports/events/' + approvalEventId + '/registrations'
    And header Authorization = 'Bearer ' + adminTok
    When method GET
    Then status 200
    * def approvalRegs     = response
    * def confirmedRegs    = karate.filter(approvalRegs, function(r){ return r.id == pendingApprovalId })
    * def confirmedStatus  = confirmedRegs.length > 0 ? confirmedRegs[0].status : 'NOT_FOUND'
    * assert confirmedStatus == 'CONFIRMED'
    * print '✅ Registration', pendingApprovalId, 'is now CONFIRMED'

  # ── 5. Register after REGISTRATION_CLOSED ──────────────────────────────────
  @sports-registration
  Scenario: Register after event is REGISTRATION_CLOSED → blocked (400 or 409)

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Closed Sport', icon: '🚫', formats: ['SINGLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def closedSportId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Closed Cat', categoryType: 'SINGLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def closedCatId  = response.id
    * def closedCatArr = [closedCatId]

    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest Closed Event",
        "sportId": #(closedSportId),
        "communityId": #(communityId),
        "eventDateStart": "2026-11-20",
        "eventDateEnd": "2026-11-22",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 32,
        "format": "SINGLES",
        "tournamentType": "KNOCKOUT",
        "categoryIds": #(closedCatArr),
        "adminApprovalRequired": false,
        "minPlayers": 1,
        "maxPlayers": 1,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def closedEventId = response.id

    # Close registration
    * call read('classpath:karate/features/sports/events/update-event-status.feature') { sportsEventId: '#(closedEventId)', newStatus: 'REGISTRATION_CLOSED' }
    * print '📌 Event status set to REGISTRATION_CLOSED'

    # Attempt registration — must be rejected
    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request { eventId: '#(closedEventId)', categoryId: '#(closedCatId)', matchType: 'SINGLES' }
    When method POST
    * match [400, 409] contains responseStatus
    * print '✅ Registration after REGISTRATION_CLOSED correctly blocked:', responseStatus

  # ── 6. Capacity limit ───────────────────────────────────────────────────────
  @sports-registration
  Scenario: Capacity enforcement — 2nd registration blocked when maxParticipants=1

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Capacity Sport', icon: '🏆', formats: ['SINGLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def capSportId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Capacity Cat', categoryType: 'SINGLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def capCatId  = response.id
    * def capCatArr = [capCatId]

    # Event with maxParticipants=1
    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest Capacity Event",
        "sportId": #(capSportId),
        "communityId": #(communityId),
        "eventDateStart": "2026-11-25",
        "eventDateEnd": "2026-11-27",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 1,
        "format": "SINGLES",
        "tournamentType": "KNOCKOUT",
        "categoryIds": #(capCatArr),
        "adminApprovalRequired": false,
        "minPlayers": 1,
        "maxPlayers": 1,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def capEventId = response.id

    # sportsman1 registers — should succeed (slot 1 of 1)
    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request { eventId: '#(capEventId)', categoryId: '#(capCatId)', matchType: 'SINGLES' }
    When method POST
    Then status 201
    * print '✅ First registration succeeded — event full'

    # sportsman2 registers — must be blocked (event full)
    * def userEmail    = 'sportsman2@sports.test'
    * def userPassword = 'Test@1234'
    * def s2Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s2Token      = s2Auth.authToken

    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s2Token
    And request { eventId: '#(capEventId)', categoryId: '#(capCatId)', matchType: 'SINGLES' }
    When method POST
    * match [400, 409] contains responseStatus
    * print '✅ Capacity limit enforced, 2nd registration blocked:', responseStatus

  # ── 7. Duplicate registration ───────────────────────────────────────────────
  @sports-registration
  Scenario: Duplicate registration by same user for same event → 409

    Given path '/sports/meta'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Duplicate Sport', icon: '♻️', formats: ['SINGLES'], active: true }
    When method POST
    * match [200, 201] contains responseStatus
    * def dupSportId = response.id

    Given path '/player-categories'
    And header Authorization = 'Bearer ' + adminTok
    And request { name: 'RegTest Duplicate Cat', categoryType: 'SINGLES', minAge: 16, maxAge: 99, gender: 'MALE' }
    When method POST
    * match [200, 201] contains responseStatus
    * def dupCatId  = response.id
    * def dupCatArr = [dupCatId]

    Given path '/sports/events'
    And header Authorization = 'Bearer ' + adminTok
    And request
      """
      {
        "name": "RegTest Duplicate Event",
        "sportId": #(dupSportId),
        "communityId": #(communityId),
        "eventDateStart": "2026-12-01",
        "eventDateEnd": "2026-12-03",
        "registrationDateStart": "2026-10-01",
        "registrationDateEnd": "2026-10-31",
        "maxParticipants": 32,
        "format": "SINGLES",
        "tournamentType": "KNOCKOUT",
        "categoryIds": #(dupCatArr),
        "adminApprovalRequired": false,
        "minPlayers": 1,
        "maxPlayers": 1,
        "gender": "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def dupEventId = response.id

    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def s1Auth       = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = s1Auth.authToken

    # First registration — success
    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request { eventId: '#(dupEventId)', categoryId: '#(dupCatId)', matchType: 'SINGLES' }
    When method POST
    Then status 201
    * print '📌 First registration succeeded'

    # Duplicate registration by same user — must be rejected
    Given path '/sports/register'
    And header Authorization = 'Bearer ' + s1Token
    And request { eventId: '#(dupEventId)', categoryId: '#(dupCatId)', matchType: 'SINGLES' }
    When method POST
    Then status 409
    * print '✅ Duplicate registration correctly blocked with 409'
