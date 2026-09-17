@ignore
Feature: Register a user for a sports event
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: sportsEventId, regCategoryId, regMatchType, regUserEmail, regUserPassword
  # Returns:  registrationId, registrationStatus

  Background:
    * url baseUrl
    * def userAuth = call read('classpath:karate/features/auth/user-login.feature')
    * def token    = userAuth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /sports/register — register user for event
    Given path '/sports/register'
    And request
      """
      {
        "eventId":    #(sportsEventId),
        "categoryId": #(regCategoryId),
        "matchType":  "#(regMatchType)"
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def regCreated      = responseStatus != 409
    * def registrationId  = regCreated ? response.id     : null
    * def registrationStatus = regCreated ? response.status : 'ALREADY_EXISTS'
    * if (!regCreated) karate.log('Registration already exists for event:', sportsEventId)
    * print '✅ Registration — status:', responseStatus, '| regId:', registrationId, '| regStatus:', registrationStatus
