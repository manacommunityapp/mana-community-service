@ignore
Feature: Update sports event status
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: sportsEventId, newStatus

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /sports/events/{id}/status — change event status
    Given path '/sports/events/' + sportsEventId + '/status'
    And param status = newStatus
    When method PUT
    * match [200, 201, 204] contains responseStatus
    * print '✅ Event', sportsEventId, 'status changed to:', newStatus
