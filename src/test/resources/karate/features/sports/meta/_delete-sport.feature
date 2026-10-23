@ignore
Feature: Soft-delete a sport meta entry (sets active=false)
  # Requires: deleteId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: DELETE /sports/meta/{id}
    Given path '/sports/meta/' + deleteId
    When method DELETE
    * match [200, 204, 404] contains responseStatus
    * print '  [delete-sport] status:', responseStatus, '| ID:', deleteId
