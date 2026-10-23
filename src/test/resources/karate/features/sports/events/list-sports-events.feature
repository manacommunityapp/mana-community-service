@sports-events @smoke
Feature: List and query sports events — GET /api/sports/events/*

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /sports/events/all — paginated event list
    Given path '/sports/events/all'
    And param page = 0
    And param size = 20
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ All sports events:', response.length

  Scenario: GET /sports/events/open?communityId= — open events
    Given path '/sports/events/open'
    And param communityId = communityId
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Open events:', response.length

  Scenario: GET /sports/events/open-all — open events community-scoped
    Given path '/sports/events/open-all'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Open-all events:', response.length

  Scenario: GET /sports/events/closed — completed/closed events
    Given path '/sports/events/closed'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Closed events:', response.length

  Scenario: GET /sports/events/mine — events created by the logged-in admin
    Given path '/sports/events/mine'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ My events:', response.length

  Scenario: GET /sports/events/community?communityId= — all community events
    Given path '/sports/events/community'
    And param communityId       = communityId
    And param includeInactive   = false
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Community events:', response.length

  Scenario: GET /sports/admin/overview — dashboard overview (tournaments + events)
    Given path '/sports/admin/overview'
    And param communityId = communityId
    When method GET
    Then status 200
    And match response == '#object'
    * print '✅ Admin overview returned'

  Scenario: GET /sports/admin/form-data — sports, categories, communities for forms
    Given path '/sports/admin/form-data'
    When method GET
    Then status 200
    And match response == '#object'
    * print '✅ Admin form-data returned'

  Scenario: GET /sports/events/{id} — fetch a single event (if any exist)
    Given path '/sports/events/all'
    And param page = 0
    And param size = 1
    When method GET
    Then status 200
    * def events  = response
    * def hasEvt  = events.length > 0
    * if (hasEvt) karate.call(read('classpath:karate/features/sports/events/_fetch-single-event.feature'), { fetchSportEventId: events[0].id })
    * print '✅ Single event fetch done, hasEvt:', hasEvt
