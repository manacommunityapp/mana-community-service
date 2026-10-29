@karate-batch @sports
Feature: Karate Batch & Class Generation

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

    # Ensure a program exists to attach batches to
    Given path '/sports/karate/programs'
    And request { name: 'Batch Test Program', description: 'For batch tests', minAge: 8, maxAge: 99 }
    When method POST
    Then status 201
    * def programId = response.id
    * print '📌 programId:', programId

  @karate-batch
  Scenario: POST /programs/{id}/batches — create a batch
    Given path '/sports/karate/programs/' + programId + '/batches'
    And request
      """
      {
        "name": "Morning Batch Jan 2026",
        "startDate": "2026-01-10",
        "daysOfWeek": "MON,WED,FRI",
        "maxCapacity": 20,
        "venue": "Community Hall A"
      }
      """
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.status == 'UPCOMING'
    And match response.enrolledCount == 0
    And match response.name == 'Morning Batch Jan 2026'
    * def batchId = response.id
    * print '✅ Batch created id:', batchId

  @karate-batch
  Scenario: GET /programs/{id}/batches — lists batches for the program
    # Create batch first
    Given path '/sports/karate/programs/' + programId + '/batches'
    And request { name: 'Evening Batch Feb 2026', startDate: '2026-02-01', daysOfWeek: 'TUE,THU', maxCapacity: 15 }
    When method POST
    Then status 201
    * def batchId = response.id

    Given path '/sports/karate/programs/' + programId + '/batches'
    When method GET
    Then status 200
    And match response == '#array'
    And match response[*].name contains 'Evening Batch Feb 2026'
    * print '✅ Batch list count:', response.length

  @karate-batch
  Scenario: PUT /batches/{id}/status — UPCOMING → ACTIVE
    Given path '/sports/karate/programs/' + programId + '/batches'
    And request { name: 'Status Test Batch', startDate: '2026-03-01', daysOfWeek: 'MON', maxCapacity: 10 }
    When method POST
    Then status 201
    * def batchId = response.id

    Given path '/sports/karate/batches/' + batchId + '/status'
    And request { status: 'ACTIVE' }
    When method PUT
    Then status 200
    And match response.status == 'ACTIVE'
    * print '✅ Batch status → ACTIVE'

  @karate-batch
  Scenario: PUT /batches/{id}/status — ACTIVE → COMPLETED
    Given path '/sports/karate/programs/' + programId + '/batches'
    And request { name: 'Complete Test Batch', startDate: '2026-04-01', daysOfWeek: 'FRI', maxCapacity: 10 }
    When method POST
    Then status 201
    * def batchId = response.id

    # First activate
    Given path '/sports/karate/batches/' + batchId + '/status'
    And request { status: 'ACTIVE' }
    When method PUT
    Then status 200

    # Then complete
    Given path '/sports/karate/batches/' + batchId + '/status'
    And request { status: 'COMPLETED' }
    When method PUT
    Then status 200
    And match response.status == 'COMPLETED'
    * print '✅ Batch status → COMPLETED'
