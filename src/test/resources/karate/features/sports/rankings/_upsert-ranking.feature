@ignore
Feature: Upsert a player ranking
  # Requires: rankUserId, rankSportId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /sports/rankings — upsert
    Given path '/sports/rankings'
    And request
      """
      {
        "userId":      #(rankUserId),
        "sportId":     #(rankSportId),
        "communityId": #(communityId),
        "season":      "CURRENT",
        "rankPosition": 1,
        "points":       950,
        "source":       "MANUAL"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * print '  [upsert-ranking] status:', responseStatus, '| userId:', rankUserId
