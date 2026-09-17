@sports-schedule @smoke
Feature: Sports schedule stats endpoints

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /sports/schedule/stats — full stats summary
    Given path '/sports/schedule/stats'
    When method GET
    Then status 200
    And match response == '#object'
    * print '✅ Schedule stats:', response

  Scenario: GET /sports/schedule/stats/total — total events count
    Given path '/sports/schedule/stats/total'
    When method GET
    Then status 200
    * print '✅ Total events:', response

  Scenario: GET /sports/schedule/stats/live — live events count
    Given path '/sports/schedule/stats/live'
    When method GET
    Then status 200
    * print '✅ Live events:', response

  Scenario: GET /sports/schedule/stats/upcoming — upcoming events count
    Given path '/sports/schedule/stats/upcoming'
    When method GET
    Then status 200
    * print '✅ Upcoming events:', response

  Scenario: GET /sports/schedule/stats/completed — completed events count
    Given path '/sports/schedule/stats/completed'
    When method GET
    Then status 200
    * print '✅ Completed events:', response

  Scenario: GET /sports/schedule/events/open — lean event list for schedule page
    Given path '/sports/schedule/events/open'
    And param communityId = communityId
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Open events (schedule):', response.length

  Scenario: GET /sports/schedule/registrations/mine — schedule page own registrations
    Given path '/sports/schedule/registrations/mine'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ My schedule registrations:', response.length
