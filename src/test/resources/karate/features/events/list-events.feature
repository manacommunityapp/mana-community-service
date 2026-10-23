@event-list @smoke
Feature: List and retrieve events

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /events — list upcoming community events
    Given path '/events'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ /events returned', response.length, 'event(s)'

  Scenario: GET /events/all — all events including past
    Given path '/events/all'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ /events/all returned', response.length, 'event(s)'

  Scenario: GET /events with type filter
    Given path '/events'
    And param type = 'CULTURAL'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ /events?type=CULTURAL returned', response.length, 'event(s)'

  Scenario: GET /events/mine — events I registered for
    Given path '/events/mine'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ /events/mine returned', response.length, 'event(s)'

  Scenario: GET /events/{id} — fetch single event (requires at least one existing event)
    # List all events first to get a valid ID
    Given path '/events/all'
    When method GET
    Then status 200
    * def allEvents = response
    * def hasEvents = allEvents.length > 0
    * if (hasEvents) karate.log('Testing single-event fetch for ID:', allEvents[0].id)

    # Only run the single-event fetch if there are events; else skip gracefully
    * def singleResp = hasEvents ? karate.call(read('classpath:karate/features/events/_fetch-single-event.feature'), { fetchId: allEvents[0].id }) : null
    * print '✅ Single-event fetch skipped (no events):', !hasEvents
