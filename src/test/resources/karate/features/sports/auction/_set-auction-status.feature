@ignore
Feature: Set auction config status
  # Requires: aucCfgId, aucStatus

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /auction/config/{id}/status?status=
    Given path '/auction/config/' + aucCfgId + '/status'
    And param status = aucStatus
    When method PUT
    * match [200, 201, 204] contains responseStatus
    * print '  [set-auction-status] status:', responseStatus, '| → ', aucStatus
