@ignore
Feature: Create a sports event for testing
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: sportId, categoryId (open singles), mixedCategoryId
  # Returns:  sportsEventId, sportsEventUuid

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /sports/events — create Cricket Karate Test event
    Given path '/sports/events'
    And request
      """
      {
        "name":                    "Cricket Karate Test Tournament 2026",
        "sportId":                 #(sportId),
        "communityId":             #(communityId),
        "eventDateStart":          "2026-10-01",
        "eventDateEnd":            "2026-10-07",
        "registrationDateStart":   "2026-09-15",
        "registrationDateEnd":     "2026-09-30",
        "maxParticipants":         32,
        "format":                  "SINGLES",
        "tournamentType":          "KNOCKOUT",
        "categoryIds":             [#(categoryId)],
        "adminApprovalRequired":   false,
        "mandatoryMixedDoubles":   false,
        "description":             "Annual community cricket tournament — Karate automation test event",
        "contactName":             "Test Organiser",
        "contactNumber":           "9999900001",
        "contactEmail":            "organiser@sports.test",
        "minPlayers":              1,
        "maxPlayers":              1,
        "gender":                  "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id   != null
    And match response.name == 'Cricket Karate Test Tournament 2026'
    * def sportsEventId   = response.id
    * def sportsEventUuid = response.uuid
    * print '✅ Sports event created — ID:', sportsEventId, '| UUID:', sportsEventUuid

  Scenario: POST /sports/events — create Mixed Doubles event (Badminton)
    Given path '/sports/meta'
    When method GET
    Then status 200
    * def badmintonList = karate.filter(response, function(s){ return s.name == 'Badminton Karate Test' })
    * def badmintonId   = badmintonList.length > 0 ? badmintonList[0].id : sportId

    Given path '/player-categories'
    When method GET
    Then status 200
    * def mixedList   = karate.filter(response, function(c){ return c.name == 'Mixed Doubles Karate Test' })
    * def mixedCatId  = mixedList.length > 0 ? mixedList[0].id : categoryId

    Given path '/sports/events'
    And header Content-Type   = 'application/json'
    And header X-Community-Id = communityId
    And request
      """
      {
        "name":                    "Badminton Mixed Doubles Karate Test 2026",
        "sportId":                 #(badmintonId),
        "communityId":             #(communityId),
        "eventDateStart":          "2026-10-10",
        "eventDateEnd":            "2026-10-12",
        "registrationDateStart":   "2026-09-15",
        "registrationDateEnd":     "2026-10-05",
        "maxParticipants":         16,
        "format":                  "MIXED_DOUBLES",
        "tournamentType":          "ROUND_ROBIN",
        "categoryIds":             [#(mixedCatId)],
        "adminApprovalRequired":   true,
        "mandatoryMixedDoubles":   true,
        "description":             "Badminton mixed doubles — Karate automation test event",
        "minPlayers":              2,
        "maxPlayers":              2,
        "gender":                  "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id != null
    * def mixedDoublesEventId = response.id
    * print '✅ Mixed doubles event created — ID:', mixedDoublesEventId
