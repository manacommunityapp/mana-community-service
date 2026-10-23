@signup @e2e @smoke
Feature: End-to-end signup and auth lifecycle

  # Orchestrates: register → login → get profile → update profile → change password → logout

  Scenario: Full user onboarding lifecycle
    * def ts        = java.lang.System.currentTimeMillis()
    * def testEmail = 'e2e_user_' + ts + '@signup.test'
    * def testPhone = '700' + (ts + '').slice(-8)

    # 1. Register
    Given url baseUrl + '/auth/register'
    And header Content-Type = 'application/json'
    And request
      """
      {
        "fullName":     "E2E Test Resident",
        "email":        "#(testEmail)",
        "phone":        "#(testPhone)",
        "password":     "Test@1234",
        "aadharNumber": "777700000001",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1995-09-10",
        "gender":       "MALE",
        "flatNo":       "404-D"
      }
      """
    When method POST
    Then status 201
    * def userId    = response.userId
    * def userToken = response.token
    * print '✅ Step 1 — Registered userId:', userId

    # 2. Login with the same credentials
    Given url baseUrl + '/auth/login'
    And header Content-Type = 'application/json'
    And request { identifier: '#(testEmail)', password: 'Test@1234' }
    When method POST
    Then status 200
    And match response.token != null
    * def refreshToken = response.refreshToken
    * print '✅ Step 2 — Login OK'

    # 3. Get my profile
    Given url baseUrl + '/users/me'
    And header Authorization  = 'Bearer ' + userToken
    And header Content-Type   = 'application/json'
    When method GET
    Then status 200
    And match response.email == testEmail
    * print '✅ Step 3 — /users/me OK'

    # 4. Update profile
    Given url baseUrl + '/profile'
    And header Authorization  = 'Bearer ' + userToken
    And header Content-Type   = 'application/json'
    And request { bio: 'Registered via E2E Karate test' }
    When method PUT
    Then status 200
    * print '✅ Step 4 — Profile update OK'

    # 5. Refresh token
    Given url baseUrl + '/auth/refresh'
    And header Content-Type = 'application/json'
    And request { refreshToken: '#(refreshToken)' }
    When method POST
    Then status 200
    And match response.token != null
    * def freshToken = response.token
    * print '✅ Step 5 — Token refresh OK'

    # 6. Logout
    Given url baseUrl + '/auth/logout'
    And header Authorization = 'Bearer ' + freshToken
    And header Content-Type  = 'application/json'
    When method POST
    Then status 200
    * print '✅ Step 6 — Logout OK'

    * print ''
    * print '═══════════════════════════════════'
    * print '✅  E2E SIGNUP FLOW PASSED'
    * print '   User ID  :', userId
    * print '   Email    :', testEmail
    * print '═══════════════════════════════════'
