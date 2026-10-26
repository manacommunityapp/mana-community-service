@karate-setup @sports
Feature: Karate Belt & Program Setup

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  @smoke @karate-setup
  Scenario: GET /api/sports/karate/belts returns 9 seeded belt levels
    Given path '/sports/karate/belts'
    When method GET
    Then status 200
    And match response == '#[9]'
    And match response[0].name == 'White'
    And match response[8].name == 'Black'
    And match each response == { id: '#number', name: '#string', sortOrder: '#number', colorHex: '#string' }
    * print '✅ 9 karate belts returned'

  @karate-setup
  Scenario: POST /api/sports/karate/programs — create Junior Karate Program
    Given path '/sports/karate/programs'
    And request { name: 'Junior Karate Program', description: 'Ages 6–14', minAge: 6, maxAge: 14 }
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.name == 'Junior Karate Program'
    And match response.status == 'DRAFT'
    * def programId = response.id
    * print '✅ Created karate program id:', programId

  @karate-setup
  Scenario: GET /api/sports/karate/programs — lists created program
    Given path '/sports/karate/programs'
    When method GET
    Then status 200
    And match response == '#array'
    And match response[*].name contains 'Junior Karate Program'
    * print '✅ Programs list returned, count:', response.length

  @karate-setup
  Scenario: GET /api/sports/karate/programs/{id} — fetch single program
    # Re-create to get a fresh id in this scenario
    Given path '/sports/karate/programs'
    And request { name: 'Senior Karate Program', description: 'Ages 15+', minAge: 15, maxAge: 99 }
    When method POST
    Then status 201
    * def newProgramId = response.id

    Given path '/sports/karate/programs/' + newProgramId
    When method GET
    Then status 200
    And match response.id == newProgramId
    * print '✅ Single program fetch ok, id:', newProgramId
