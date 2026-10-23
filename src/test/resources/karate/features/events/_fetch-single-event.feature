@ignore
Feature: Fetch a single event by ID
  # Requires: fetchId (the event's DB id)

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /events/{id}
    Given path '/events/' + fetchId
    When method GET
    Then status 200
    And match response.id == fetchId
    And match response.title != null
    * print '✅ Event detail — ID:', response.id, '| title:', response.title, '| status:', response.status
