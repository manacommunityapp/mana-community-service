@sports-users
Feature: Create 2 sports test users (sportsman1 + sportsman2) for sports module flows

  # Uses distinct email domain @sports.test so cleanup can target them precisely.
  # sportsman1 → MALE  (primary / captain candidate)
  # sportsman2 → FEMALE (partner for mixed doubles)

  Background:
    * url baseUrl

  Scenario: Register sportsman1 (male)
    Given path '/auth/register'
    And header Content-Type = 'application/json'
    And request
      """
      {
        "fullName":     "Sports User One",
        "email":        "sportsman1@sports.test",
        "phone":        "80000000001",
        "password":     "Test@1234",
        "aadharNumber": "200000000001",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1995-06-15",
        "gender":       "MALE",
        "flatNo":       "101-S"
      }
      """
    When method POST
    * match [201, 409] contains responseStatus
    * def sports1Created = responseStatus == 201
    * if (!sports1Created) karate.log('sportsman1 already exists')
    * print '✅ sportsman1 status:', responseStatus

  Scenario: Register sportsman2 (female)
    Given path '/auth/register'
    And header Content-Type = 'application/json'
    And request
      """
      {
        "fullName":     "Sports User Two",
        "email":        "sportsman2@sports.test",
        "phone":        "80000000002",
        "password":     "Test@1234",
        "aadharNumber": "200000000002",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1997-03-22",
        "gender":       "FEMALE",
        "flatNo":       "102-S"
      }
      """
    When method POST
    * match [201, 409] contains responseStatus
    * def sports2Created = responseStatus == 201
    * if (!sports2Created) karate.log('sportsman2 already exists')
    * print '✅ sportsman2 status:', responseStatus

  Scenario: Verify both users can log in
    Given path '/auth/login'
    And header Content-Type = 'application/json'
    And request { identifier: 'sportsman1@sports.test', password: 'Test@1234' }
    When method POST
    Then status 200
    * def sports1Token = response.token
    * print '✅ sportsman1 token obtained'

    Given path '/auth/login'
    And header Content-Type = 'application/json'
    And request { identifier: 'sportsman2@sports.test', password: 'Test@1234' }
    When method POST
    Then status 200
    * def sports2Token = response.token
    * print '✅ sportsman2 token obtained'
