@ignore
Feature: Devotee registers for Ganesh Pooja slot
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: createdEventId, seva14EveningId, userEmail, userPassword

  # Expects these variables from the calling context:
  #   createdEventId   — parent event ID
  #   seva14EveningId  — the Sept 14 evening seva ID
  #   userEmail        — devotee's login email
  #   userPassword     — devotee's password
  #
  # Entity field notes (as of V89–V93 migrations):
  #   poojaSlotTime    — was poojaStartTime in earlier design; use poojaSlotTime
  #   poojaSevaTimeSlotsId — the time-slot row id (not poojaSevaId)
  #   registrationSource   — auto-set to SELF for normal user registrations
  #   status column on time_slots now has 'OPEN' default (V93)

  Background:
    * url baseUrl
    # Log in as the devotee user
    * def userAuth = call read('classpath:karate/features/auth/user-login.feature')
    * def token    = userAuth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Fetch available schedules for Sept 14 evening seva
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    * assert response.length > 0
    * def schedule    = response[0]
    * def scheduleId  = schedule.id
    # PoojaScheduleDto fields: availableDevotees / availableFamilies (not availableSlots)
    * print 'Schedule found — ID:', scheduleId, '| availableDevotees:', schedule.availableDevotees

  Scenario: Reserve a slot (pessimistic lock, idempotent)
    # Re-fetch schedules; timeSlotConfigId comes from PoojaScheduleDto.timeSlotConfigId
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    And match response[0].status == 'OPEN'
    * def scheduleId       = response[0].id
    * def timeSlotConfigId = response[0].timeSlotConfigId

    # Reserve — unique idempotency key prevents double-booking (V89: DB-level partial unique index)
    * def idemKey = java.util.UUID.randomUUID().toString()
    Given path '/events/pooja-schedules/' + scheduleId + '/reserve'
    And request { idempotencyKey: '#(idemKey)', familyCount: 1, devoteeCount: 1 }
    When method POST
    Then status 200
    And match response.reservationId != null
    And match response.tokenNumber   != null
    * def reservationId = response.reservationId
    * print '✅ Reservation ID:', reservationId, '| token:', response.tokenNumber

  Scenario: Complete pooja registration (confirm reservation)
    # Re-fetch schedule and re-reserve (shared state not carried across scenarios)
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    * def scheduleId       = response[0].id
    * def timeSlotConfigId = response[0].timeSlotConfigId

    * def idemKey = java.util.UUID.randomUUID().toString()
    Given path '/events/pooja-schedules/' + scheduleId + '/reserve'
    And request { idempotencyKey: '#(idemKey)', familyCount: 1, devoteeCount: 1 }
    When method POST
    Then status 200
    * def reservationId = response.reservationId

    # Create the pooja registration (saved in event_pooja_user_registrations)
    # poojaSlotTime replaces poojaStartTime; poojaSevaTimeSlotsId replaces poojaSevaId
    Given path '/events/pooja-registrations'
    And request
      """
      {
        "eventId":              #(createdEventId),
        "poojaSevaTimeSlotsId": #(timeSlotConfigId),
        "scheduleId":           #(scheduleId),
        "reservationId":        #(reservationId),
        "participantName":      "Devotee User 1",
        "gotram":               "Kashyapa",
        "devoteeCount":         1,
        "poojaSlotDate":        "2026-09-14",
        "poojaSlotTime":        "19:00",
        "paymentMethod":        "MANUAL",
        "paymentStatus":        "PENDING",
        "notes":                "First-time devotee"
      }
      """
    When method POST
    Then status 201
    And match response.regCode            != null
    And match response.status             == 'CONFIRMED'
    And match response.registrationSource == 'SELF'
    And match response.overrideUsed       == false
    * def regCode = response.regCode
    * print '✅ Registration complete — reg code:', regCode

  Scenario: Duplicate registration is rejected (V89/V91 unique constraint)
    # Same user attempting same slot again must get a conflict
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    * def scheduleId       = response[0].id
    * def timeSlotConfigId = response[0].timeSlotConfigId

    * def idemKey2 = java.util.UUID.randomUUID().toString()
    Given path '/events/pooja-schedules/' + scheduleId + '/reserve'
    And request { idempotencyKey: '#(idemKey2)', familyCount: 1, devoteeCount: 1 }
    When method POST
    Then status 200
    * def reservationId2 = response.reservationId

    Given path '/events/pooja-registrations'
    And request
      """
      {
        "eventId":              #(createdEventId),
        "poojaSevaTimeSlotsId": #(timeSlotConfigId),
        "scheduleId":           #(scheduleId),
        "reservationId":        #(reservationId2),
        "participantName":      "Devotee User 1",
        "gotram":               "Kashyapa",
        "devoteeCount":         1,
        "poojaSlotDate":        "2026-09-14",
        "poojaSlotTime":        "19:00",
        "paymentMethod":        "MANUAL"
      }
      """
    When method POST
    * match [409, 400] contains responseStatus
    * print '✅ Duplicate registration correctly rejected:', response

  Scenario: Verify registration appears in My Registrations
    Given path '/events/pooja-registrations/my'
    When method GET
    Then status 200
    * assert response.length >= 1
    * print '✅ My registrations count:', response.length
