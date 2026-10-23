@ignore
Feature: Fetch a single tournament by ID
  # Requires: fetchTournamentId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /tournaments/{id}
    Given path '/tournaments/' + fetchTournamentId
    When method GET
    Then status 200
    And match response.id   == fetchTournamentId
    And match response.name != null
    * print '  [fetch-tournament] ID:', response.id, '| name:', response.name
