@ignore
Feature: Fetch a single sports event by ID
  # Requires: fetchSportEventId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /sports/events/{id}
    Given path '/sports/events/' + fetchSportEventId
    When method GET
    Then status 200
    And match response.id   == fetchSportEventId
    And match response.name != null
    * print '  [fetch-sport-event] ID:', response.id, '| name:', response.name, '| status:', response.status
