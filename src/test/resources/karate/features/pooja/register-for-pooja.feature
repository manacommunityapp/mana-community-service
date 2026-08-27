@pooja @registration @smoke
Feature: Devotee registers for Ganesh Pooja slot

  # Expects these variables from the calling context:
  #   createdEventId   — parent event ID
  #   seva14EveningId  — the Sept 14 evening seva ID
  #   userEmail        — devotee's login email
  #   userPassword     — devotee's password

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
    And match response.length > 0
    * def schedule    = response[0]
    * def scheduleId  = schedule.id
    * print 'Schedule found — ID:', scheduleId, '| available:', schedule.availableSlots

  Scenario: Reserve a slot (pessimistic lock, idempotent)
    # Re-fetch schedules to get scheduleId
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    * def scheduleId = response[0].id

    # Reserve — unique idempotency key prevents double-booking
    * def idemKey = java.util.UUID.randomUUID().toString()
    Given path '/events/pooja-schedules/' + scheduleId + '/reserve'
    And request { idempotencyKey: '#(idemKey)', devoteeCount: 1 }
    When method POST
    Then status 200
    And match response.reservationId != null
    * def reservationId = response.reservationId
    * print '✅ Reservation ID:', reservationId

  Scenario: Complete pooja registration (confirm reservation)
    # Re-fetch schedule and re-reserve (shared state not carried across scenarios)
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    * def scheduleId = response[0].id

    * def idemKey = java.util.UUID.randomUUID().toString()
    Given path '/events/pooja-schedules/' + scheduleId + '/reserve'
    And request { idempotencyKey: '#(idemKey)', devoteeCount: 1 }
    When method POST
    Then status 200
    * def reservationId = response.reservationId

    # Create the pooja registration (saved in event_pooja_user_registrations)
    Given path '/events/pooja-registrations'
    And request
      """
      {
        "eventId":         #(createdEventId),
        "poojaSevaId":     #(seva14EveningId),
        "scheduleId":      #(scheduleId),
        "reservationId":   "#(reservationId)",
        "participantName": "Devotee User 1",
        "gotram":          "Kashyapa",
        "devoteeCount":    1,
        "poojaSlotDate":   "2026-09-14",
        "poojaStartTime":  "19:00",
        "paymentMethod":   "MANUAL",
        "paymentStatus":   "PENDING",
        "notes":           "First-time devotee"
      }
      """
    When method POST
    Then status 201
    And match response.regCode != null
    And match response.status  == 'CONFIRMED'
    * def regCode = response.regCode
    * print '✅ Registration complete — reg code:', regCode

  Scenario: Verify registration appears in My Registrations
    Given path '/events/pooja-registrations/my'
    When method GET
    Then status 200
    And match response.length >= 1
    * print '✅ My registrations count:', response.length
