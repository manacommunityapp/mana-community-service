@ignore
Feature: Tournament content — announcements, gallery, timeline
  # Called by: sports/e2e/cricket-tournament-e2e.feature
  # Requires: tournamentId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  # ── Announcements ─────────────────────────────────────────────────────────

  Scenario: POST /tournaments/{id}/announcements — add announcement
    Given path '/tournaments/' + tournamentId + '/announcements'
    And request
      """
      {
        "title":     "Registration Now Open",
        "content":   "Register for Cricket Karate Test Open 2026 now. Slots filling fast!",
        "icon":      "🏏",
        "sortOrder": 1
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.content != null
    * def announcementId = response.id
    * print '✅ Announcement created — ID:', announcementId

  Scenario: GET /tournaments/{id}/announcements — verify announcement in list
    Given path '/tournaments/' + tournamentId + '/announcements'
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 1
    * print '✅ Announcements count:', response.length

  Scenario: DELETE /tournaments/{id}/announcements/{aid} — remove announcement
    Given path '/tournaments/' + tournamentId + '/announcements'
    When method GET
    Then status 200
    * def announcements = response
    * if (announcements.length > 0) karate.call(read('classpath:karate/features/sports/tournament/_delete-announcement.feature'), { delTournamentId: tournamentId, delAnnouncementId: announcements[0].id })
    * print '✅ Announcement delete step done'

  # ── Gallery ───────────────────────────────────────────────────────────────

  Scenario: POST /tournaments/{id}/gallery — add gallery image
    Given path '/tournaments/' + tournamentId + '/gallery'
    And request
      """
      {
        "title":     "Tournament Banner",
        "imageUrl":  "https://example.com/banner.jpg",
        "bgColor":   "#FF6600",
        "sortOrder": 1
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def galleryImageId = response.id
    * print '✅ Gallery image created — ID:', galleryImageId

  Scenario: GET /tournaments/{id}/gallery — verify gallery
    Given path '/tournaments/' + tournamentId + '/gallery'
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 1
    * print '✅ Gallery images count:', response.length

  # ── Timeline ──────────────────────────────────────────────────────────────

  Scenario: POST /tournaments/{id}/timeline — add timeline entry
    Given path '/tournaments/' + tournamentId + '/timeline'
    And request
      """
      {
        "title":       "Registration Opens",
        "entryDate":   "2026-09-15",
        "dateLabel":   "15 Sep 2026",
        "description": "Online registration portal goes live at 10:00 AM",
        "sortOrder":   1
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * def timelineEntryId = response.id
    * print '✅ Timeline entry created — ID:', timelineEntryId

  Scenario: POST /tournaments/{id}/timeline — add second entry
    Given path '/tournaments/' + tournamentId + '/timeline'
    And request
      """
      {
        "title":       "Tournament Begins",
        "entryDate":   "2026-10-01",
        "dateLabel":   "1 Oct 2026",
        "description": "Group stage matches start at 9:00 AM",
        "sortOrder":   2
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    * print '✅ Second timeline entry created'

  Scenario: GET /tournaments/{id}/timeline — verify timeline order
    Given path '/tournaments/' + tournamentId + '/timeline'
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 2
    * print '✅ Timeline entries count:', response.length
