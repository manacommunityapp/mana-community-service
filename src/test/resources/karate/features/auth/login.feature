@ignore
Feature: Admin login helper — call this from other features to obtain a Bearer token

  Scenario: Admin login
    Given url baseUrl + '/auth/login'
    And header Content-Type = 'application/json'
    And request { identifier: '#(adminIdentifier)', password: '#(adminPassword)' }
    When method POST
    Then status 200
    And match response.token != null
    * def authToken    = response.token
    * def refreshToken = response.refreshToken
