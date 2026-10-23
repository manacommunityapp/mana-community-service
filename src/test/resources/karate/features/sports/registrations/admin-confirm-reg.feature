@ignore
Feature: Admin confirms a pending sports registration
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: confirmSportsRegId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /sports/registrations/{id}/confirm — confirm registration
    Given path '/sports/registrations/' + confirmSportsRegId + '/confirm'
    When method PUT
    * match [200, 201, 204, 409] contains responseStatus
    * print '✅ Confirm registration', confirmSportsRegId, '→ status:', responseStatus
