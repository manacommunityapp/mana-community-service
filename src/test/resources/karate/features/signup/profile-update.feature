@signup @profile @smoke
Feature: User profile update and password change

  # Covers: PUT /api/profile, POST /api/auth/change-password

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization = 'Bearer ' + token

  Scenario: Update profile fields
    Given path '/profile'
    And request
      """
      {
        "bio":      "Community automation test admin",
        "linkedIn": "https://linkedin.com/in/testadmin",
        "twitter":  "@testadmin"
      }
      """
    When method PUT
    Then status 200
    And match response != null
    * print '✅ Profile updated'

  Scenario: Read back updated profile
    Given path '/profile'
    When method GET
    Then status 200
    And match response != null
    * print '✅ Profile read back:', response

  Scenario: Change password (admin user)
    Given path '/auth/change-password'
    And request
      """
      {
        "currentPassword": "#(adminPassword)",
        "newPassword":     "#(adminPassword)"
      }
      """
    When method POST
    Then status 200
    And match response.success == true
    * print '✅ Password change accepted'
