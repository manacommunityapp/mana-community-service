@ignore
Feature: Helper — register and login karate test users

  # Creates karateuser1@karate.test (student) and returns their token + userId.
  # Idempotent: 409 on re-register is treated as success.

  Scenario:
    * url baseUrl

    # Register student user
    Given path '/auth/register'
    And header Content-Type = 'application/json'
    And request
      """
      {
        "fullName":     "Karate Student One",
        "email":        "karateuser1@karate.test",
        "phone":        "90000000011",
        "password":     "Test@1234",
        "aadharNumber": "300000000011",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "2005-04-10",
        "gender":       "MALE",
        "flatNo":       "201-K"
      }
      """
    When method POST
    * match [201, 409] contains responseStatus

    # Login as student and capture userId
    * def userEmail    = 'karateuser1@karate.test'
    * def userPassword = 'Test@1234'
    * def userAuth = call read('classpath:karate/features/auth/user-login.feature')
    * def karateUserToken  = userAuth.authToken

    # Fetch userId via /users/me
    Given path '/users/me'
    And header Authorization  = 'Bearer ' + karateUserToken
    And header X-Community-Id = communityId
    When method GET
    Then status 200
    * def karateUserId = response.id
    * print '✅ Karate student userId:', karateUserId
