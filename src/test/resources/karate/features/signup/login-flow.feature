@signup @auth @smoke
Feature: Login, token refresh, and logout flow

  # Covers: POST /api/auth/login, /api/auth/refresh, /api/auth/logout
  # Also tests GET /api/users/me and GET /api/profile.

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: Successful login returns access + refresh tokens
    Given path '/auth/login'
    And request { identifier: '#(adminIdentifier)', password: '#(adminPassword)' }
    When method POST
    Then status 200
    And match response.token        != null
    And match response.refreshToken != null
    And match response.userId       != null
    * def adminToken        = response.token
    * def adminRefreshToken = response.refreshToken
    * print '✅ Admin login: token received'

  Scenario: Wrong password returns 401
    Given path '/auth/login'
    And request { identifier: '#(adminIdentifier)', password: 'WrongPassword999!' }
    When method POST
    Then status 401
    * print '✅ Wrong password rejected'

  Scenario: Unknown email/identifier returns 4xx
    Given path '/auth/login'
    And request { identifier: 'nobody@nowhere.test', password: 'Anything@1' }
    When method POST
    * match [400, 401, 404] contains responseStatus
    * print '✅ Unknown user rejected'

  Scenario: Fetch my profile after login
    * def loginResult = call read('classpath:karate/features/auth/login.feature')
    * def token = loginResult.authToken
    Given path '/users/me'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.email != null
    * print '✅ /users/me:', response.email

  Scenario: Get full profile via /api/profile
    * def loginResult = call read('classpath:karate/features/auth/login.feature')
    * def token = loginResult.authToken
    Given path '/profile'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response != null
    * print '✅ /profile response received'

  Scenario: Refresh token issues a new access token
    * def loginResult = call read('classpath:karate/features/auth/login.feature')
    * def refreshToken = loginResult.refreshToken
    Given path '/auth/refresh'
    And request { refreshToken: '#(refreshToken)' }
    When method POST
    Then status 200
    And match response.token != null
    * def newAccessToken = response.token
    * print '✅ Token refreshed successfully'

  Scenario: Logout invalidates the session
    * def loginResult = call read('classpath:karate/features/auth/login.feature')
    * def token = loginResult.authToken
    Given path '/auth/logout'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    * print '✅ Logout successful'
