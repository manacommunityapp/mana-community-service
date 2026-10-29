@karate-enroll @sports
Feature: Karate Enrollment & Attendance

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

    # Create and activate an enrollment test batch
    Given path '/sports/karate/programs'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Enroll Test Program', description: 'For enroll tests', minAge: 6, maxAge: 99 }
    When method POST
    Then status 201
    * def programId = response.id

    Given path '/sports/karate/programs/' + programId + '/batches'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Enroll Test Batch', startDate: '2026-05-01', daysOfWeek: 'MON,WED,FRI', maxCapacity: 20 }
    When method POST
    Then status 201
    * def batchId = response.id

    Given path '/sports/karate/batches/' + batchId + '/status'
    And header Authorization = 'Bearer ' + token
    And request { status: 'ACTIVE' }
    When method PUT
    Then status 200

    # Create and login as student user
    * def students = call read('classpath:karate/features/sports/karate/_create-karate-users.feature')
    * def userToken    = students.karateUserToken
    * def studentId    = students.karateUserId

  @karate-enroll
  Scenario: POST /batches/{id}/enroll — student enrolls in active batch
    Given path '/sports/karate/batches/' + batchId + '/enroll'
    And header Authorization = 'Bearer ' + userToken
    And request {}
    When method POST
    Then status 201
    And match response.status == 'ACTIVE'
    And match response.user.id == studentId
    * print '✅ Student enrolled in batch:', batchId

  @karate-enroll
  Scenario: POST /batches/{id}/enroll — duplicate enroll returns 400
    # First enroll
    Given path '/sports/karate/batches/' + batchId + '/enroll'
    And header Authorization = 'Bearer ' + userToken
    And request {}
    When method POST
    * match [201, 400] contains responseStatus

    # Second enroll — must be rejected
    Given path '/sports/karate/batches/' + batchId + '/enroll'
    And header Authorization = 'Bearer ' + userToken
    And request {}
    When method POST
    Then status 400
    * print '✅ Duplicate enroll rejected with 400'

  @karate-enroll
  Scenario: GET /batches/{id}/attendance — returns attendance summary
    # Enroll first
    Given path '/sports/karate/batches/' + batchId + '/enroll'
    And header Authorization = 'Bearer ' + userToken
    And request {}
    When method POST
    * match [201, 400] contains responseStatus

    # Fetch attendance as admin
    Given path '/sports/karate/batches/' + batchId + '/attendance'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.batchId == batchId
    And match response.totalEnrolled == '#number'
    And match response.enrollees == '#array'
    And match each response.enrollees == { userId: '#number', userName: '#string', classesAttended: '#number', totalClasses: '#number', attendancePct: '#number', lowAttendanceAlert: '#boolean' }
    * print '✅ Attendance response, enrolled:', response.totalEnrolled

  @karate-enroll
  Scenario: Enroll in non-active batch returns 400
    # Create a UPCOMING (not yet activated) batch
    Given path '/sports/karate/programs/' + programId + '/batches'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Locked Batch', startDate: '2027-01-01', daysOfWeek: 'SAT', maxCapacity: 5 }
    When method POST
    Then status 201
    * def lockedBatchId = response.id

    # Mark CANCELLED
    Given path '/sports/karate/batches/' + lockedBatchId + '/status'
    And header Authorization = 'Bearer ' + token
    And request { status: 'CANCELLED' }
    When method PUT
    Then status 200

    Given path '/sports/karate/batches/' + lockedBatchId + '/enroll'
    And header Authorization = 'Bearer ' + userToken
    And request {}
    When method POST
    Then status 400
    * print '✅ Enroll in CANCELLED batch correctly rejected'
