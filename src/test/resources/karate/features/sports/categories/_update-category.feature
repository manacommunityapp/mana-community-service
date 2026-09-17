@ignore
Feature: Update a player category
  # Requires: updateCatId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /player-categories/{id}
    Given path '/player-categories/' + updateCatId
    And request
      """
      {
        "name":         "Open Karate Test",
        "categoryType": "SINGLES",
        "description":  "Open singles — updated description",
        "minAge":       16,
        "maxAge":       99,
        "gender":       "MALE"
      }
      """
    When method PUT
    * match [200, 201] contains responseStatus
    * print '  [update-category] status:', responseStatus, '| ID:', updateCatId
