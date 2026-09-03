@ignore
Feature: Update an event (PUT /api/events/{id})
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: eventId, adminToken, communityId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Update event notes field (partial update)
    Given path '/events/' + eventId
    And request
      """
      {
        "title":       "Ganesh Mahotsav 2026",
        "notes":       "Updated: Dress code traditional. No footwear in mandap. Prasadam after each pooja. Online registration closes Sept 13.",
        "capacity":    150,
        "maxAttendees": 150
      }
      """
    When method PUT
    Then status 200
    And match response.id       == eventId
    And match response.capacity == 150
    * print '✅ Event updated — capacity now:', response.capacity
