@ignore
Feature: Helper — fetch karate belts

  Scenario:
    * url baseUrl
    * def adminToken = karate.callSingle('classpath:karate/features/signup/login-flow.feature@admin').token
    Given path '/api/sports/karate/belts'
    And header Authorization = 'Bearer ' + adminToken
    When method GET
    Then status 200
    * def belts = response
