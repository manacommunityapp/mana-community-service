@ignore
Feature: Auction config — create, update, status transitions, stats
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: sportId, sportsEventId
  # Returns:  auctionConfigId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /auction/config — create auction config (DRAFT)
    Given path '/auction/config'
    And request
      """
      {
        "sportId":              #(sportId),
        "eventId":              #(sportsEventId),
        "seasonName":           "Cricket Karate Test Auction 2026",
        "auctionFormat":        "SNAKE",
        "totalTeams":           4,
        "totalPlayers":         12,
        "budgetPerTeam":        100000,
        "basePrice":            1000,
        "bidIncrementDefault":  500,
        "bidTimerSeconds":      30,
        "rtmEnabled":           false,
        "categories":           ["BATSMEN", "BOWLERS", "ALL_ROUNDERS"]
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def auctionConfigId = response.id
    * print '✅ Auction config created — ID:', auctionConfigId

  Scenario: GET /auction/config/{id} — fetch config detail
    Given path '/auction/config'
    And param sportId = sportId
    When method GET
    Then status 200
    * def configs = response
    * def ourConfig = karate.filter(configs, function(c){ return c.seasonName == 'Cricket Karate Test Auction 2026' })
    * def hasConfig = ourConfig.length > 0
    * def cfgId     = hasConfig ? ourConfig[0].id : null

    * if (cfgId != null) karate.call(read('classpath:karate/features/sports/auction/_fetch-auction-config.feature'), { fetchAuctionConfigId: cfgId })
    * print '✅ Auction config fetch done, found:', hasConfig

  Scenario: GET /auction/config/{id}/stats — registration count and stats
    Given path '/auction/config'
    And param sportId = sportId
    When method GET
    Then status 200
    * def ourConfig = karate.filter(response, function(c){ return c.seasonName == 'Cricket Karate Test Auction 2026' })
    * def cfgId = ourConfig.length > 0 ? ourConfig[0].id : null
    * if (cfgId == null) karate.log('No auction config found — skipping stats')

    * if (cfgId != null) karate.call(read('classpath:karate/features/sports/auction/_auction-stats.feature'), { statsCfgId: cfgId })
    * print '✅ Auction stats done'

  Scenario: PUT /auction/config/{id}/status → ACTIVE (DRAFT→ACTIVE)
    Given path '/auction/config'
    And param sportId = sportId
    When method GET
    Then status 200
    * def ourConfig = karate.filter(response, function(c){ return c.seasonName == 'Cricket Karate Test Auction 2026' })
    * def cfgId = ourConfig.length > 0 ? ourConfig[0].id : null
    * if (cfgId == null) karate.log('No config to activate')

    * if (cfgId != null) karate.call(read('classpath:karate/features/sports/auction/_set-auction-status.feature'), { aucCfgId: cfgId, aucStatus: 'ACTIVE' })
    * print '✅ Auction status → ACTIVE'
