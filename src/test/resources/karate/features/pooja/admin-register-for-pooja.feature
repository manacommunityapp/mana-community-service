@ignore
Feature: Admin registers a devotee for a Pooja slot on their behalf
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: createdEventId, seva14EveningId, targetDevoteeUserId

  # Tests POST /api/events/pooja-registrations/admin-create (commit 906a71f)
  # Expects: createdEventId, seva14EveningId, targetDevoteeUserId (ID of devotee1)
  # Admin bypasses capacity/duplicate checks (adminOverride=true, overrideUsed=true in DB)

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Admin fetches available schedules
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    And match response[0].status == 'OPEN'
    * def adminScheduleId       = response[0].id
    * def adminTimeSlotConfigId = response[0].poojaSevaTimeSlotsId
    * print 'Admin schedule ID:', adminScheduleId

  Scenario: Admin creates registration on behalf of a devotee (admin-create endpoint)
    # Fetch schedule first
    Given path '/events/pooja-schedules'
    And param poojaId = seva14EveningId
    And param date    = '2026-09-14'
    When method GET
    Then status 200
    * def adminScheduleId       = response[0].id
    * def adminTimeSlotConfigId = response[0].poojaSevaTimeSlotsId

    # Use admin-create — no reservation needed; admin bypasses the reserve step
    Given path '/events/pooja-registrations/admin-create'
    And request
      """
      {
        "targetUserId":        #(targetDevoteeUserId),
        "overrideReason":      "Admin helping devotee who was unable to register online",
        "eventId":             #(createdEventId),
        "scheduleId":          #(adminScheduleId),
        "poojaSevaTimeSlotsId": #(adminTimeSlotConfigId),
        "participantName":     "Devotee User 2",
        "gotram":              "Bharadwaja",
        "devoteeCount":        1,
        "poojaSlotDate":       "2026-09-14",
        "poojaSlotTime":       "19:00",
        "paymentMethod":       "MANUAL",
        "paymentStatus":       "PENDING"
      }
      """
    When method POST
    Then status 201
    And match response.regCode            != null
    And match response.status             == 'CONFIRMED'
    And match response.registrationSource == 'ADMIN'
    And match response.overrideUsed       == true
    * print '✅ Admin registration created — reg code:', response.regCode

  Scenario: Admin searches users by name within community
    Given path '/events/pooja-registrations/admin/user-search'
    And param q           = 'Devotee'
    And param communityId = communityId
    When method GET
    Then status 200
    * assert response.length >= 1
    * print '✅ Admin user-search returned', response.length, 'users'
