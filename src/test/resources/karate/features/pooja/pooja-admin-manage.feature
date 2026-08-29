@ignore
Feature: Admin pooja registration management — list, summary, participants
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: eventId, seva14EveningId, adminToken, communityId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /events/pooja-registrations — admin full list
    Given path '/events/pooja-registrations'
    And param poojaSevaId = seva14EveningId
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 1
    * def firstReg = response[0]
    And match firstReg.id        != null
    And match firstReg.regCode   != null
    And match firstReg.status    != null
    * print '✅ Admin list — registrations:', response.length, '| first regCode:', firstReg.regCode

  Scenario: GET /events/pooja-registrations/summary — lightweight summary list
    Given path '/events/pooja-registrations/summary'
    And param poojaSevaId = seva14EveningId
    When method GET
    Then status 200
    And match response == '#array'
    * assert response.length >= 1
    * def summ = response[0]
    # PoojaRegistrationSummaryResponse fields
    And match summ.id              != null
    And match summ.regCode         != null
    And match summ.participantName != null
    And match summ.status          != null
    * print '✅ Summary list count:', response.length, '| first participant:', summ.participantName

  Scenario: GET /events/pooja-registrations/{id}/participants — booking participants
    # Get a registration ID from the full list
    Given path '/events/pooja-registrations'
    And param poojaSevaId = seva14EveningId
    When method GET
    Then status 200
    * def regId = response[0].id

    Given path '/events/pooja-registrations/' + regId + '/participants'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Participants for registration', regId, ':', response.length

  Scenario: GET /events/pooja-registrations/{id} — single registration detail
    Given path '/events/pooja-registrations'
    And param poojaSevaId = seva14EveningId
    When method GET
    Then status 200
    * def regId = response[0].id

    Given path '/events/pooja-registrations/' + regId
    When method GET
    Then status 200
    And match response.id      == regId
    And match response.regCode != null
    And match response.status  != null
    * print '✅ Single registration — regCode:', response.regCode, '| source:', response.registrationSource
