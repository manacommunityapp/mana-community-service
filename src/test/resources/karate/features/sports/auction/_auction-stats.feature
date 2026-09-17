@ignore
Feature: Fetch auction config stats and registration count
  # Requires: statsCfgId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /auction/config/{id}/stats
    Given path '/auction/config/' + statsCfgId + '/stats'
    When method GET
    * match [200, 404] contains responseStatus
    * print '  [auction-stats] status:', responseStatus

  Scenario: GET /auction/config/{id}/registration-count
    Given path '/auction/config/' + statsCfgId + '/registration-count'
    When method GET
    * match [200, 404] contains responseStatus
    * print '  [auction-reg-count] status:', responseStatus
