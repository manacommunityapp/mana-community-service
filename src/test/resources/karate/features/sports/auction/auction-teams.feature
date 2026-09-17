@ignore
Feature: Auction teams — create teams and add players
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: auctionConfigId, sportsEventId
  # Returns:  teamAlphaId, teamBetaId, playerIds

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /auction/teams — create Team Alpha
    Given path '/auction/teams'
    And request
      """
      {
        "configId":    #(auctionConfigId),
        "teamName":    "Team Alpha Karate Test",
        "ownerName":   "Alpha Captain",
        "colorHex":    "#FF0000",
        "totalBudget": 100000
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def teamAlphaId = response.id
    * print '✅ Team Alpha created — ID:', teamAlphaId

  Scenario: POST /auction/teams — create Team Beta
    Given path '/auction/teams'
    And request
      """
      {
        "configId":    #(auctionConfigId),
        "teamName":    "Team Beta Karate Test",
        "ownerName":   "Beta Captain",
        "colorHex":    "#0000FF",
        "totalBudget": 100000
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def teamBetaId = response.id
    * print '✅ Team Beta created — ID:', teamBetaId

  Scenario: POST /auction/players — add Player 1 (Batsman)
    Given path '/auction/players'
    And request
      """
      {
        "playerName": "Karate Batsman One",
        "configId":   #(auctionConfigId),
        "category":   "BATSMEN",
        "playerRole": "TOP_ORDER",
        "age":        25,
        "basePrice":  2000,
        "matches":    45,
        "runs":       1800,
        "strikeRate": 142.5
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def player1Id = response.id
    * print '✅ Player 1 (Batsman) created — ID:', player1Id

  Scenario: POST /auction/players — add Player 2 (Bowler)
    Given path '/auction/players'
    And request
      """
      {
        "playerName": "Karate Bowler One",
        "configId":   #(auctionConfigId),
        "category":   "BOWLERS",
        "playerRole": "FAST",
        "age":        27,
        "basePrice":  2000,
        "matches":    40,
        "wickets":    62,
        "economy":    7.8
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def player2Id = response.id
    * print '✅ Player 2 (Bowler) created — ID:', player2Id

  Scenario: POST /auction/players — add Player 3 (All-Rounder)
    Given path '/auction/players'
    And request
      """
      {
        "playerName": "Karate AllRounder One",
        "configId":   #(auctionConfigId),
        "category":   "ALL_ROUNDERS",
        "playerRole": "ALL_ROUNDER",
        "age":        24,
        "basePrice":  3000,
        "matches":    35,
        "runs":       900,
        "wickets":    30,
        "strikeRate": 128.0
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def player3Id = response.id
    * print '✅ Player 3 (All-Rounder) created — ID:', player3Id

  Scenario: GET /auction/teams/{configId} — verify teams in roster
    Given path '/auction/teams/' + auctionConfigId
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 2
    * print '✅ Teams in auction:', response.length
