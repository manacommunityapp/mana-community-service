@event-dashboard @smoke
Feature: Event dashboard endpoints — stats, analytics, pending-actions

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /events/dashboard/stats — summary counts
    Given path '/events/dashboard/stats'
    When method GET
    Then status 200
    And match response == '#object'
    * print '✅ Dashboard stats:', response

  Scenario: GET /events/dashboard/analytics — analytics data
    Given path '/events/dashboard/analytics'
    When method GET
    Then status 200
    And match response == '#object'
    * print '✅ Dashboard analytics returned'

  Scenario: GET /events/dashboard/pending-actions — items needing admin attention
    Given path '/events/dashboard/pending-actions'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Dashboard pending-actions count:', response.length
