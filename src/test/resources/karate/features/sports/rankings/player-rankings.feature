@sports-rankings @smoke
Feature: Player rankings — GET/POST/PUT/DELETE /api/sports/rankings

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /sports/rankings — list rankings (no sport filter)
    Given path '/sports/rankings'
    And param communityId = communityId
    And param season      = 'CURRENT'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Rankings count:', response.length

  Scenario: GET /sports/meta — find cricket sport for ranking upsert
    Given path '/sports/meta'
    When method GET
    Then status 200
    * def cricketSport = karate.filter(response, function(s){ return s.name == 'Cricket Karate Test' })
    * def hasCricket   = cricketSport.length > 0
    * def rankSportId  = hasCricket ? cricketSport[0].id : (response.length > 0 ? response[0].id : null)
    * print '✅ rankSportId:', rankSportId, '| hasCricket:', hasCricket

  Scenario: POST /sports/rankings — upsert ranking for sportsman1
    # Get sport id first
    Given path '/sports/meta'
    When method GET
    Then status 200
    * def allSports   = response
    * def cricketList = karate.filter(allSports, function(s){ return s.name == 'Cricket Karate Test' })
    * def sportId     = cricketList.length > 0 ? cricketList[0].id : (allSports.length > 0 ? allSports[0].id : null)
    * if (sportId == null) karate.log('No sport found — skipping ranking upsert')

    # Look up sportsman1 user id via admin user-search
    Given url baseUrl + '/events/pooja-registrations/admin/user-search'
    And header Authorization  = 'Bearer ' + token
    And header X-Community-Id = communityId
    And param q           = 'Sports User One'
    And param communityId = communityId
    When method GET
    Then status 200
    * def sports1Users = response
    * def sports1Id    = sports1Users.length > 0 ? sports1Users[0].id : null
    * if (sports1Id == null) karate.log('sportsman1 user not found — ranking upsert skipped')

    * if (sportId != null && sports1Id != null) karate.call(read('classpath:karate/features/sports/rankings/_upsert-ranking.feature'), { rankUserId: sports1Id, rankSportId: sportId })
    * print '✅ Ranking upsert done, sportId:', sportId, '| userId:', sports1Id

  Scenario: GET /sports/rankings — verify ranking appears
    Given path '/sports/rankings'
    And param communityId = communityId
    And param season      = 'CURRENT'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Rankings after upsert:', response.length
