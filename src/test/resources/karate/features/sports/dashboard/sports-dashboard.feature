@sports-dashboard @smoke
Feature: Sports dashboard endpoints — stats, upcoming, registrations, notifications

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /sports/dashboard/stats — aggregate counts
    Given path '/sports/dashboard/stats'
    When method GET
    Then status 200
    And match response == '#object'
    * print '✅ Dashboard stats:', response

  Scenario: GET /sports/dashboard/upcoming — upcoming tournaments
    Given path '/sports/dashboard/upcoming'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Upcoming tournaments count:', response.length

  Scenario: GET /sports/dashboard/open-tournaments — events open for registration
    Given path '/sports/dashboard/open-tournaments'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Open tournaments count:', response.length

  Scenario: GET /sports/dashboard/closed-tournaments — completed/closed events
    Given path '/sports/dashboard/closed-tournaments'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Closed tournaments count:', response.length

  Scenario: GET /sports/dashboard/my-registrations — admin's own registrations
    Given path '/sports/dashboard/my-registrations'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ My registrations count:', response.length

  Scenario: GET /sports/dashboard/notifications — top active notifications
    Given path '/sports/dashboard/notifications'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Dashboard notifications count:', response.length
