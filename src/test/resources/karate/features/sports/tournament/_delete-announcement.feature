@ignore
Feature: Delete a tournament announcement
  # Requires: delTournamentId, delAnnouncementId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: DELETE /tournaments/{tid}/announcements/{aid}
    Given path '/tournaments/' + delTournamentId + '/announcements/' + delAnnouncementId
    When method DELETE
    * match [200, 204, 404] contains responseStatus
    * print '  [delete-announcement] status:', responseStatus
