@pooja-types @smoke
Feature: Pooja types CRUD — GET and POST /api/events/pooja-types

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /events/pooja-types — list all types for community
    Given path '/events/pooja-types'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Pooja types found:', response.length

  Scenario: POST /events/pooja-types — create a new pooja type
    Given path '/events/pooja-types'
    And request { name: 'Ganesh Abhishekam', description: 'Ritual bathing ceremony of the deity' }
    When method POST
    # 200 or 201 depending on service implementation
    * match [200, 201] contains responseStatus
    And match response.name == 'Ganesh Abhishekam'
    * print '✅ Pooja type created — ID:', response.id

  Scenario: GET /events/pooja-types — new type appears in list
    Given path '/events/pooja-types'
    When method GET
    Then status 200
    * def found = karate.filter(response, function(t){ return t.name == 'Ganesh Abhishekam' })
    * assert found.length >= 1
    * print '✅ Created pooja type visible in list'
