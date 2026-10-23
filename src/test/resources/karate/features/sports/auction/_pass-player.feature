@ignore
Feature: Pass (mark unsold) a player in live auction
  # Requires: passPlayerId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /auction/live/{playerId}/pass
    Given path '/auction/live/' + passPlayerId + '/pass'
    When method POST
    * match [200, 201, 204, 400] contains responseStatus
    * print '  [pass-player] status:', responseStatus, '| playerId:', passPlayerId
