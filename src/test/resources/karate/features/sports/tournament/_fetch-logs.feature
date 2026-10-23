@ignore
Feature: Fetch schedule generation logs for a config
  # Requires: logConfigId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /tournament/logs/config/{configId}
    Given path '/tournament/logs/config/' + logConfigId
    When method GET
    * match [200, 404] contains responseStatus
    * if (responseStatus == 200) karate.match(response, '#array')
    * print '  [fetch-logs] status:', responseStatus, '| logs:', (responseStatus == 200 ? response.length : 0)
