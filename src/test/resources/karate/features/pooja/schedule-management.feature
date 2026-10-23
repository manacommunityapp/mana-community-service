@ignore
Feature: Admin pooja schedule management — create, update, status-patch, reservations list
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: seva14EveningId, adminToken, communityId
  # Returns:  managedScheduleId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /events/pooja-schedules — admin creates an explicit schedule
    Given path '/events/pooja-schedules'
    And request
      """
      {
        "poojaId":        #(seva14EveningId),
        "scheduleDate":   "2026-09-14",
        "startTime":      "19:00",
        "endTime":        "20:00",
        "familyCapacity": 30,
        "devoteeCapacity": 120,
        "status":         "OPEN"
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def managedScheduleId = (responseStatus == 409) ? null : response.id
    * print '✅ Admin-created schedule status:', responseStatus, '| ID:', managedScheduleId

  Scenario: GET /events/pooja-schedules — verify schedule appears in list
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    When method GET
    Then status 200
    * assert response.length > 0
    * def schedId = response[0].id
    * print '✅ Schedules for seva:', response.length

  Scenario: PATCH /events/pooja-schedules/{id}/status — change status to BLOCKED then back to OPEN
    # Get a schedule to patch
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    When method GET
    Then status 200
    * def patchId = response[0].id

    # Block the schedule
    Given path '/events/pooja-schedules/' + patchId + '/status'
    And param status = 'BLOCKED'
    When method PATCH
    Then status 200
    * print '✅ Schedule status set to BLOCKED — ID:', patchId

    # Re-open for remaining e2e steps
    Given path '/events/pooja-schedules/' + patchId + '/status'
    And param status = 'OPEN'
    When method PATCH
    Then status 200
    * print '✅ Schedule re-opened — ID:', patchId

  Scenario: GET /events/pooja-schedules/{scheduleId}/reservations — admin views reservations
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    When method GET
    Then status 200
    * def firstSchedId = response[0].id

    Given path '/events/pooja-schedules/' + firstSchedId + '/reservations'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Schedule reservations count:', response.length
