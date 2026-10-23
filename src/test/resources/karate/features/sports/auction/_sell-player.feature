@ignore
Feature: Mark a player as sold in live auction
  # Requires: sellConfigId, sellPlayerId, sellTeamId, soldPrice

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /auction/live/sold
    Given path '/auction/live/sold'
    And request
      """
      {
        "configId":  #(sellConfigId),
        "playerId":  #(sellPlayerId),
        "teamId":    #(sellTeamId),
        "soldPrice": #(soldPrice)
      }
      """
    When method POST
    * match [200, 201, 400, 409] contains responseStatus
    * print '  [sell-player] status:', responseStatus, '| playerId:', sellPlayerId, '| team:', sellTeamId, '| price:', soldPrice
