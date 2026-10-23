@ignore
Feature: Update a sport meta entry
  # Requires: updateId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /sports/meta/{id}
    Given path '/sports/meta/' + updateId
    And request { name: 'Cricket Karate Test', icon: '🏏', iconUrl: null, active: true }
    When method PUT
    * match [200, 201] contains responseStatus
    * print '  [update-sport] status:', responseStatus, '| ID:', updateId
