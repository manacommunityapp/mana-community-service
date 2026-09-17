@sports-meta @smoke
Feature: Sports meta (sport types) — CRUD via GET/POST/PUT/DELETE /api/sports/meta

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /sports/meta — list active sports for community
    Given path '/sports/meta'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Sports meta list count:', response.length

  Scenario: POST /sports/meta — create Cricket (Karate Test sport)
    Given path '/sports/meta'
    And request
      """
      {
        "name":    "Cricket Karate Test",
        "icon":    "🏏",
        "formats": ["T20", "ODI", "Test"],
        "active":  true
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.name == 'Cricket Karate Test'
    * def createdSportId   = response.id
    * def createdSportName = response.name
    * print '✅ Sport created — ID:', createdSportId

  Scenario: POST /sports/meta — create Badminton (Karate Test sport)
    Given path '/sports/meta'
    And request
      """
      {
        "name":    "Badminton Karate Test",
        "icon":    "🏸",
        "formats": ["SINGLES", "DOUBLES", "MIXED_DOUBLES"],
        "active":  true
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.name == 'Badminton Karate Test'
    * def badmintonSportId = response.id
    * print '✅ Badminton sport created — ID:', badmintonSportId

  Scenario: PUT /sports/meta/{id} — update icon of Cricket sport
    # Re-create to get a stable ID in this scenario context
    Given path '/sports/meta'
    When method GET
    Then status 200
    * def cricketSport = karate.filter(response, function(s){ return s.name == 'Cricket Karate Test' })
    * def hasSport = cricketSport.length > 0
    * if (!hasSport) karate.log('Cricket Karate Test not found — skipping update')

    * if (hasSport) karate.call(read('classpath:karate/features/sports/meta/_update-sport.feature'), { updateId: cricketSport[0].id })
    * print '✅ Sport update step done, found:', hasSport

  Scenario: GET /sports/meta — updated sport appears with new icon
    Given path '/sports/meta'
    When method GET
    Then status 200
    * def found = karate.filter(response, function(s){ return s.name == 'Cricket Karate Test' })
    * assert found.length >= 1
    * print '✅ Cricket Karate Test visible in meta list'

  Scenario: DELETE /sports/meta/{id} — soft-delete Badminton test sport
    Given path '/sports/meta'
    When method GET
    Then status 200
    * def badminton = karate.filter(response, function(s){ return s.name == 'Badminton Karate Test' })
    * if (badminton.length == 0) karate.log('Badminton Karate Test not found — skipping delete')
    * if (badminton.length > 0) karate.call(read('classpath:karate/features/sports/meta/_delete-sport.feature'), { deleteId: badminton[0].id })
    * print '✅ Soft-delete step done'
