@ignore
Feature: Fetch generated schedule for a config
  # Requires: schedConfigId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /tournament/{configId}/schedule
    Given path '/tournament/' + schedConfigId + '/schedule'
    When method GET
    * match [200, 404] contains responseStatus
    * print '  [fetch-schedule] status:', responseStatus, '| configId:', schedConfigId
