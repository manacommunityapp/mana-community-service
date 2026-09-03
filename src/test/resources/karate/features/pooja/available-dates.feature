@ignore
Feature: Pooja schedule — available dates and single-schedule fetch
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: seva14EveningId

  Background:
    * url baseUrl
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /events/pooja-schedules/available-dates — dates with open capacity
    Given path '/events/pooja-schedules/available-dates'
    And param poojaId = seva14EveningId
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Available dates for seva', seva14EveningId, ':', response

  Scenario: GET /events/pooja-schedules — schedules list for Sept 14
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length > 0
    * def sched = response[0]
    # Verify all PoojaScheduleDto fields are present
    And match sched.id                 != null
    And match sched.poojaId            == seva14EveningId
    And match sched.scheduleDate       != null
    And match sched.startTime          != null
    And match sched.status             != null
    And match sched.availableDevotees  != null
    And match sched.availableFamilies  != null
    * def singleScheduleId = sched.id
    * print '✅ Schedule — ID:', sched.id, '| status:', sched.status, '| availableDevotees:', sched.availableDevotees

  Scenario: GET /events/pooja-schedules/{id} — single schedule with live availability
    # Need a scheduleId — re-fetch schedules
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    When method GET
    Then status 200
    * def schedId = response[0].id

    Given path '/events/pooja-schedules/' + schedId
    When method GET
    Then status 200
    And match response.id             == schedId
    And match response.availableDevotees != null
    * print '✅ Single schedule — availableDevotees:', response.availableDevotees, '| availableFamilies:', response.availableFamilies
