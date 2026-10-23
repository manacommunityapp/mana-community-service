@ignore
Feature: Place a bid in a live auction
  # Requires: bidConfigId, bidPlayerId, bidTeamId, bidAmount

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /auction/live/bid
    Given path '/auction/live/bid'
    And request
      """
      {
        "configId":  #(bidConfigId),
        "playerId":  #(bidPlayerId),
        "teamId":    #(bidTeamId),
        "bidAmount": #(bidAmount)
      }
      """
    When method POST
    * match [200, 201, 400, 409] contains responseStatus
    * print '  [place-bid] status:', responseStatus, '| teamId:', bidTeamId, '| amount:', bidAmount
