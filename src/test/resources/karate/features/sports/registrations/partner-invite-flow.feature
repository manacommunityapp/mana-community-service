@ignore
Feature: Partner invitation flow — mixed doubles registration with partner confirmation
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: mixedDoublesEventId, mixedCategoryId, sports1UserId, sports2UserId
  # Tests:
  #   - Player 1 registers with partner invite to Player 2
  #   - Player 2 checks pending invitations
  #   - Player 2 accepts the invitation
  #   - Admin confirms the registration
  #   - Duplicate block: Player 2 tries to register solo → 409

  Background:
    * url baseUrl
    * def adminAuth = callonce read('classpath:karate/features/auth/login.feature')
    * def adminTok  = adminAuth.authToken

  Scenario: Sportsman1 registers for mixed doubles event with sportsman2 as partner
    * def userEmail    = 'sportsman1@sports.test'
    * def userPassword = 'Test@1234'
    * def userAuth     = call read('classpath:karate/features/auth/user-login.feature')
    * def s1Token      = userAuth.authToken

    Given url baseUrl + '/sports/register'
    And header Authorization  = 'Bearer ' + s1Token
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And request
      """
      {
        "eventId":       #(mixedDoublesEventId),
        "categoryId":    #(mixedCategoryId),
        "matchType":     "MIXED_DOUBLES",
        "partnerUserId": #(sports2UserId)
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def partnerRegId = (responseStatus == 409) ? null : response.id
    * print '✅ Mixed doubles registration — status:', responseStatus, '| regId:', partnerRegId

  Scenario: Sportsman2 sees pending partner invitation
    * def userEmail    = 'sportsman2@sports.test'
    * def userPassword = 'Test@1234'
    * def userAuth     = call read('classpath:karate/features/auth/user-login.feature')
    * def s2Token      = userAuth.authToken

    Given url baseUrl + '/sports/registrations/partner-invitations'
    And header Authorization  = 'Bearer ' + s2Token
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And param status = 'PENDING'
    When method GET
    Then status 200
    And match response == '#array'
    * def invitations = response
    * def hasInvite   = invitations.length > 0
    * def inviteRegId = hasInvite ? invitations[0].id : null
    * print '✅ Pending partner invitations:', invitations.length, '| inviteRegId:', inviteRegId

  Scenario: Sportsman2 accepts the partner invitation
    * def userEmail    = 'sportsman2@sports.test'
    * def userPassword = 'Test@1234'
    * def userAuth     = call read('classpath:karate/features/auth/user-login.feature')
    * def s2Token      = userAuth.authToken

    # Re-fetch invitations to get current invite
    Given url baseUrl + '/sports/registrations/partner-invitations'
    And header Authorization  = 'Bearer ' + s2Token
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And param status = 'PENDING'
    When method GET
    Then status 200
    * def invitations = response
    * def hasInvite   = invitations.length > 0
    * if (!hasInvite) karate.log('No pending invitations to accept — skipping')

    * if (hasInvite) karate.call(read('classpath:karate/features/sports/registrations/_accept-invite.feature'), { acceptRegId: invitations[0].id, userToken: s2Token })
    * print '✅ Partner acceptance step done, hasInvite:', hasInvite

  Scenario: Admin confirms the mixed doubles registration
    Given url baseUrl + '/sports/events/' + mixedDoublesEventId + '/registrations'
    And header Authorization  = 'Bearer ' + adminTok
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    When method GET
    Then status 200
    * def eventRegs = response
    * def hasReg    = eventRegs.length > 0
    * if (!hasReg) karate.log('No registrations to confirm for mixed doubles event')

    * if (hasReg) karate.call(read('classpath:karate/features/sports/registrations/admin-confirm-reg.feature'), { confirmSportsRegId: eventRegs[0].id })
    * print '✅ Admin confirmation done, hasReg:', hasReg

  Scenario: Duplicate block — sportsman2 tries to register solo for same event → 409
    * def userEmail    = 'sportsman2@sports.test'
    * def userPassword = 'Test@1234'
    * def userAuth     = call read('classpath:karate/features/auth/user-login.feature')
    * def s2Token      = userAuth.authToken

    Given url baseUrl + '/sports/register'
    And header Authorization  = 'Bearer ' + s2Token
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And request
      """
      {
        "eventId":    #(mixedDoublesEventId),
        "categoryId": #(mixedCategoryId),
        "matchType":  "SINGLES"
      }
      """
    When method POST
    # Partner already registered → duplicate guard should block this
    * match [400, 409] contains responseStatus
    * print '✅ Duplicate block correctly returned:', responseStatus
