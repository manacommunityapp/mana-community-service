@ignore
Feature: Devotee user login helper
  # Callers must set userEmail and userPassword before calling this feature.
  # Returns: authToken (Bearer token for the logged-in user)

  Scenario: Login as devotee user
    Given url baseUrl + '/auth/login'
    And header Content-Type = 'application/json'
    And request { identifier: '#(userEmail)', password: '#(userPassword)' }
    When method POST
    Then status 200
    And match response.token != null
    * def authToken = response.token
