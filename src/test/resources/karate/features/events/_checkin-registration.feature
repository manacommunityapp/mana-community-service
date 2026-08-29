@ignore
Feature: Check-in a single event registration
  # Requires: checkInRegId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /events/registrations/{regId}/check-in
    Given path '/events/registrations/' + checkInRegId + '/check-in'
    And param checkedIn = true
    When method PUT
    * match [200, 201, 204, 409] contains responseStatus
    * print '✅ Check-in status:', responseStatus
