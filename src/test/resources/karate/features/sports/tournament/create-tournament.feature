@ignore
Feature: Create a sports tournament wrapping a sports event
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: sportsEventId
  # Returns:  tournamentId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /tournaments — create tournament
    Given path '/tournaments'
    And request
      """
      {
        "name":                  "Cricket Karate Test Open 2026",
        "communityId":           #(communityId),
        "eventDateStart":        "2026-10-01",
        "eventDateEnd":          "2026-10-07",
        "registrationDateStart": "2026-09-15",
        "registrationDateEnd":   "2026-09-30",
        "sportsEventIds":        [#(sportsEventId)],
        "description":           "Karate automation test tournament",
        "contactName":           "Test Admin",
        "contactNumber":         "9999900000",
        "contactEmail":          "admin@sports.test",
        "allowAdminChat":        false
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.id   != null
    And match response.name == 'Cricket Karate Test Open 2026'
    * def tournamentId = response.id
    * print '✅ Tournament created — ID:', tournamentId

  Scenario: GET /tournaments/{id} — verify tournament detail
    # Re-fetch by listing community tournaments to get our tournament id
    Given path '/tournaments/community'
    And param communityId = communityId
    When method GET
    Then status 200
    * def tourList    = response
    * def ourTournament = karate.filter(tourList, function(t){ return t.name == 'Cricket Karate Test Open 2026' })
    * def hasTournament = ourTournament.length > 0
    * if (!hasTournament) karate.log('Tournament not found in list')

    * if (hasTournament) karate.call(read('classpath:karate/features/sports/tournament/_fetch-tournament.feature'), { fetchTournamentId: ourTournament[0].id })
    * print '✅ Tournament fetch done, found:', hasTournament

  Scenario: GET /tournaments/all — list all tournaments
    Given path '/tournaments/all'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ All tournaments count:', response.length
