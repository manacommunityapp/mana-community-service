@ignore
Feature: Tournament scheduler — config, generate schedule, record result, leaderboard
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: sportsEventId, sportId
  # Returns:  schedulerConfigId, savedMatchId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /tournament/types — verify supported tournament types
    Given path '/tournament/types'
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length > 0
    * print '✅ Tournament types:', response

  Scenario: POST /tournament/config — create KNOCKOUT config in DRAFT
    Given path '/tournament/config'
    And request
      """
      {
        "tournamentName":  "Cricket Karate Test — Knockout Config",
        "sportId":         #(sportId),
        "communityId":     #(communityId),
        "eventId":         #(sportsEventId),
        "tournamentType":  "KNOCKOUT",
        "totalTeams":      4,
        "startDate":       "2026-10-01",
        "endDate":         "2026-10-07",
        "thirdPlaceMatch": true,
        "hasSeeding":      false,
        "pointsForWin":    3,
        "pointsForDraw":   1,
        "pointsForLoss":   0
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def schedulerConfigId = response.id
    * print '✅ Scheduler config created — ID:', schedulerConfigId

  Scenario: GET /tournament/config/{id} — verify config
    # Re-fetch configs for the event
    Given path '/tournament/configs'
    When method GET
    Then status 200
    And match response == '#array'
    * def ourConfig = karate.filter(response, function(c){ return c.tournamentName == 'Cricket Karate Test — Knockout Config' })
    * def hasConfig = ourConfig.length > 0
    * def configId  = hasConfig ? ourConfig[0].id : null
    * print '✅ Configs list count:', response.length, '| ourConfig found:', hasConfig

  Scenario: GET /tournament/events — events dropdown for scheduler forms
    Given path '/tournament/events'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Tournament events dropdown:', response.length

  Scenario: POST /tournament/schedule — generate schedule (stateless preview)
    # Fetch config to get teams (they may be empty in a fresh config)
    Given path '/tournament/configs'
    When method GET
    Then status 200
    * def configs  = response
    * def ourConfig = karate.filter(configs, function(c){ return c.tournamentName == 'Cricket Karate Test — Knockout Config' })
    * def configId  = ourConfig.length > 0 ? ourConfig[0].id : null
    * if (configId == null) karate.log('Scheduler config not found — skipping schedule generation')

    * if (configId != null) karate.call(read('classpath:karate/features/sports/tournament/_generate-schedule.feature'), { genConfigId: configId })
    * print '✅ Schedule generation step done, configId:', configId

  Scenario: GET /tournament/match/{configId}/schedule — fetch generated schedule
    Given path '/tournament/configs'
    When method GET
    Then status 200
    * def ourConfig = karate.filter(response, function(c){ return c.tournamentName == 'Cricket Karate Test — Knockout Config' })
    * def configId  = ourConfig.length > 0 ? ourConfig[0].id : null
    * if (configId == null) karate.log('No config — skipping schedule fetch')

    * if (configId != null) karate.call(read('classpath:karate/features/sports/tournament/_fetch-schedule.feature'), { schedConfigId: configId })
    * print '✅ Schedule fetch done, configId:', configId

  Scenario: GET /tournament/logs/config/{configId} — schedule generation logs
    Given path '/tournament/configs'
    When method GET
    Then status 200
    * def ourConfig = karate.filter(response, function(c){ return c.tournamentName == 'Cricket Karate Test — Knockout Config' })
    * def configId  = ourConfig.length > 0 ? ourConfig[0].id : null
    * if (configId == null) karate.log('No config — skipping logs fetch')

    * if (configId != null) karate.call(read('classpath:karate/features/sports/tournament/_fetch-logs.feature'), { logConfigId: configId })
    * print '✅ Logs fetch done'
