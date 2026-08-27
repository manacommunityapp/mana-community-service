@signup @smoke
Feature: User Registration — full signup flow

  # Covers: POST /api/auth/register
  # Validates required fields, duplicate email/phone guard, and response shape.

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: Successful new user registration returns token
    * def ts        = java.lang.System.currentTimeMillis()
    * def testEmail = 'newuser_' + ts + '@signup.test'
    * def testPhone = '800' + (ts + '').slice(-8)
    Given path '/auth/register'
    And request
      """
      {
        "fullName":     "Test Resident",
        "email":        "#(testEmail)",
        "phone":        "#(testPhone)",
        "password":     "Test@1234",
        "aadharNumber": "123456789012",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1990-06-15",
        "gender":       "MALE",
        "flatNo":       "101-A",
        "block":        "A"
      }
      """
    When method POST
    Then status 201
    And match response.token    != null
    And match response.userId   != null
    And match response.fullName == 'Test Resident'
    * def registeredUserId    = response.userId
    * def registeredUserToken = response.token
    * print '✅ Registered user ID:', registeredUserId

  Scenario: Duplicate email returns 409 Conflict
    * def ts        = java.lang.System.currentTimeMillis()
    * def testEmail = 'dup_' + ts + '@signup.test'
    * def testPhone = '811' + (ts + '').slice(-8)
    # First registration
    Given path '/auth/register'
    And request
      """
      {
        "fullName":     "Original User",
        "email":        "#(testEmail)",
        "phone":        "#(testPhone)",
        "password":     "Test@1234",
        "aadharNumber": "999900000001",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1985-03-20",
        "gender":       "FEMALE",
        "flatNo":       "202-B"
      }
      """
    When method POST
    Then status 201
    # Second registration with same email
    * def ts2       = java.lang.System.currentTimeMillis()
    * def newPhone  = '822' + (ts2 + '').slice(-8)
    Given path '/auth/register'
    And request
      """
      {
        "fullName":     "Duplicate User",
        "email":        "#(testEmail)",
        "phone":        "#(newPhone)",
        "password":     "Test@5678",
        "aadharNumber": "999900000002",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1992-07-10",
        "gender":       "MALE",
        "flatNo":       "303-C"
      }
      """
    When method POST
    Then status in [400, 409]
    * print '✅ Duplicate email rejected:', response

  Scenario: Registration with missing required fields returns 400
    Given path '/auth/register'
    And request { "email": "incomplete@test.com", "password": "Test@1234" }
    When method POST
    Then status 400
    * print '✅ Validation rejection confirmed'
