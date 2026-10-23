@ignore
Feature: General event registration (non-pooja) for a community event
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: eventId, adminToken, communityId
  # Returns:  regId (the registration DB id)

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Admin registers for the main event (POST /events/{id}/register)
    Given path '/events/' + eventId + '/register'
    And request {}
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def regId = (responseStatus == 409) ? null : response.id
    * print '✅ Event registration status:', responseStatus, '| regId:', regId

  Scenario: GET /events/{id}/registrations — admin lists all registrations
    Given path '/events/' + eventId + '/registrations'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Event registrations count:', response.length
    * def firstReg = response.length > 0 ? response[0] : null

  Scenario: Confirm a registration (PUT /events/registrations/{regId}/confirm)
    # Fetch registrations to get a valid regId for confirm/check-in
    Given path '/events/' + eventId + '/registrations'
    When method GET
    Then status 200
    * def regs = response
    * def hasReg = regs.length > 0
    * if (!hasReg) karate.log('No registrations to confirm — skipping')

    * def confirmed = false
    * if (hasReg) karate.set('confirmed', true)
    * if (hasReg) karate.call(read('classpath:karate/features/events/_confirm-registration.feature'), { confirmRegId: regs[0].id })
    * print '✅ Registration confirm step done, hasReg:', hasReg

  Scenario: Check-in a registration (PUT /events/registrations/{regId}/check-in)
    Given path '/events/' + eventId + '/registrations'
    When method GET
    Then status 200
    * def regs = response
    * if (regs.length > 0) karate.call(read('classpath:karate/features/events/_checkin-registration.feature'), { checkInRegId: regs[0].id })
    * print '✅ Check-in step done'

  Scenario: Unregister from event (DELETE /events/{id}/register)
    # Only attempt if admin self-registered in scenario 1
    Given path '/events/' + eventId + '/register'
    When method DELETE
    * match [200, 204, 404] contains responseStatus
    * print '✅ Unregister status:', responseStatus
