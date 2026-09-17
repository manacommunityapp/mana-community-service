@ignore
Feature: Fetch bid history for a player
  # Requires: bidHistoryPlayerId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /auction/live/bids/{playerId}
    Given path '/auction/live/bids/' + bidHistoryPlayerId
    When method GET
    Then status 200
    And match response == '#array'
    * print '  [bid-history] bids for player', bidHistoryPlayerId, ':', response.length
