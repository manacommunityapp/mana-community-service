@karate-grading @sports
Feature: Karate Grading Exam & Belt Promotion

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

    # Fetch belt list to resolve Yellow Belt id
    Given path '/sports/karate/belts'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    * def yellowBeltId = response[1].id   // index 1 = Yellow (sort_order 2)
    * print '📌 yellowBeltId:', yellowBeltId

    # Create program + batch + enroll student
    Given path '/sports/karate/programs'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Grading Test Program', description: 'For grading tests', minAge: 6, maxAge: 99 }
    When method POST
    Then status 201
    * def programId = response.id

    Given path '/sports/karate/programs/' + programId + '/batches'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Grading Test Batch', startDate: '2026-06-01', daysOfWeek: 'MON,WED,FRI', maxCapacity: 20 }
    When method POST
    Then status 201
    * def batchId = response.id

    Given path '/sports/karate/batches/' + batchId + '/status'
    And header Authorization = 'Bearer ' + token
    And request { status: 'ACTIVE' }
    When method PUT
    Then status 200

    # Student user setup
    * def students     = call read('classpath:karate/features/sports/karate/_create-karate-users.feature')
    * def userToken    = students.karateUserToken
    * def studentId    = students.karateUserId

    # Enroll student
    Given path '/sports/karate/batches/' + batchId + '/enroll'
    And header Authorization = 'Bearer ' + userToken
    And request {}
    When method POST
    * match [201, 400] contains responseStatus

  @karate-grading
  Scenario: POST /batches/{id}/exams — create grading exam
    Given path '/sports/karate/batches/' + batchId + '/exams'
    And header Authorization = 'Bearer ' + token
    And request
      """
      {
        "examTitle": "White to Yellow Belt Exam",
        "examDate": "2026-08-15",
        "targetBeltId": #(yellowBeltId),
        "notes": "First grading exam for Junior program"
      }
      """
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.status == 'SCHEDULED'
    And match response.examTitle == 'White to Yellow Belt Exam'
    * def examId = response.id
    * print '✅ Exam created id:', examId

  @karate-grading
  Scenario: GET /batches/{id}/exams — lists exams for the batch
    # Create an exam first
    Given path '/sports/karate/batches/' + batchId + '/exams'
    And header Authorization = 'Bearer ' + token
    And request { examTitle: 'List Test Exam', examDate: '2026-09-01', targetBeltId: '#(yellowBeltId)' }
    When method POST
    Then status 201

    Given path '/sports/karate/batches/' + batchId + '/exams'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Exams list count:', response.length

  @karate-grading
  Scenario: POST /exams/{id}/results — record a PASS result and award belt
    # Create exam
    Given path '/sports/karate/batches/' + batchId + '/exams'
    And header Authorization = 'Bearer ' + token
    And request { examTitle: 'Pass Test Exam', examDate: '2026-10-01', targetBeltId: '#(yellowBeltId)' }
    When method POST
    Then status 201
    * def examId = response.id

    # Record PASS with belt award
    Given path '/sports/karate/exams/' + examId + '/results'
    And header Authorization = 'Bearer ' + token
    And request
      """
      {
        "userId": #(studentId),
        "result": "PASS",
        "score": 88,
        "awardBelt": true,
        "examinerNotes": "Excellent form and technique"
      }
      """
    When method POST
    Then status 201
    And match response.result == 'PASS'
    And match response.beltAwarded == true
    And match response.awardedBelt.id == yellowBeltId
    * print '✅ Belt awarded: Yellow'

  @karate-grading
  Scenario: POST /exams/{id}/results — idempotent on re-submit (upsert)
    Given path '/sports/karate/batches/' + batchId + '/exams'
    And header Authorization = 'Bearer ' + token
    And request { examTitle: 'Idempotent Test Exam', examDate: '2026-11-01', targetBeltId: '#(yellowBeltId)' }
    When method POST
    Then status 201
    * def examId = response.id

    # First submit
    Given path '/sports/karate/exams/' + examId + '/results'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(studentId)', result: 'PASS', score: 85, awardBelt: true }
    When method POST
    Then status 201
    * def firstResultId = response.id

    # Re-submit with updated score — should upsert, same result
    Given path '/sports/karate/exams/' + examId + '/results'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(studentId)', result: 'PASS', score: 90, awardBelt: true }
    When method POST
    Then status 201
    And match response.id == firstResultId
    And match response.score == 90
    * print '✅ Upsert idempotency confirmed, updated score: 90'

  @karate-grading
  Scenario: GET /exams/{id}/results — lists results for exam
    Given path '/sports/karate/batches/' + batchId + '/exams'
    And header Authorization = 'Bearer ' + token
    And request { examTitle: 'Results List Exam', examDate: '2026-12-01', targetBeltId: '#(yellowBeltId)' }
    When method POST
    Then status 201
    * def examId = response.id

    Given path '/sports/karate/exams/' + examId + '/results'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(studentId)', result: 'FAIL', score: 42 }
    When method POST
    Then status 201

    Given path '/sports/karate/exams/' + examId + '/results'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response == '#array'
    And match each response == { id: '#number', result: '#string', beltAwarded: '#boolean' }
    * print '✅ Results list count:', response.length
