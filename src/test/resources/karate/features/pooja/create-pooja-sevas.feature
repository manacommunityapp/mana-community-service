@ignore
Feature: Create Ganesh Pooja seva sub-events for Mahotsav 2026
  # Called by: e2e/ganesh-mahotsav-full-flow.feature
  # Requires: createdEventId (the parent event ID)
  # Returns:  seva14EveningId, sevaMainDaysId, seva19MorningId

  # Slot design:
  #   Sept 14        → Evening only  (19:00–20:00)   — opening day
  #   Sept 15–18     → Morning + Evening (10:00–11:00, 19:00–20:00) — main festival days
  #   Sept 19        → Morning only  (10:00–11:00)   — visarjan day
  #
  # Each seva: 120 slots, free (registration required)

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId
    # createdEventId must be set by the calling feature or e2e scenario

  # ── September 14 — Evening only (opening day) ───────────────────────────────

  Scenario: Sept 14 — Ganesh Pooja evening only (opening day)
    Given path '/events/pooja-sevas'
    And request
      """
      {
        "mainEventId":       #(createdEventId),
        "name":              "Ganesh Pooja — Opening Evening",
        "type":              "Ganesh Pooja",
        "date":              "2026-09-14",
        "multiDay":          false,
        "duration":          60,
        "slots":             120,
        "needsRegistration": true,
        "isFree":            true,
        "timeSlotConfig": [
          {
            "slotDate":  "2026-09-14",
            "startTime": "19:00",
            "endTime":   "20:00",
            "title":     "Ganesh Pooja — Evening Aarti",
            "slotCount": 120
          }
        ]
      }
      """
    When method POST
    Then status 201
    And match response.id                              != null
    And match response.timeSlotConfig[0].startTime     == '19:00'
    And match response.timeSlotConfig[0].endTime       == '20:00'
    And match response.timeSlotConfig[0].slotCount     == 120
    * def seva14EveningId = response.id
    * print '✅ Sept 14 Evening seva ID:', seva14EveningId

  # ── September 15–18 — Morning and Evening (main festival days) ──────────────

  Scenario: Sept 15–18 — Ganesh Pooja morning and evening (main days, multi-day)
    Given path '/events/pooja-sevas'
    And request
      """
      {
        "mainEventId":       #(createdEventId),
        "name":              "Ganesh Pooja — Main Festival Days",
        "type":              "Ganesh Pooja",
        "date":              "2026-09-15",
        "endDate":           "2026-09-18",
        "multiDay":          true,
        "startTimes":        ["10:00", "19:00"],
        "duration":          60,
        "slots":             120,
        "needsRegistration": true,
        "isFree":            true
      }
      """
    When method POST
    Then status 201
    And match response.id != null
    # 4 days × 2 time slots = 8 generated time-slot records
    And match response.timeSlotConfig.length == 8
    # First slot should be Sept 15 morning
    And match response.timeSlotConfig[0].slotDate  == '2026-09-15'
    And match response.timeSlotConfig[0].startTime == '10:00'
    And match response.timeSlotConfig[0].endTime   == '11:00'
    And match response.timeSlotConfig[0].slotCount == 120
    # Second slot should be Sept 15 evening
    And match response.timeSlotConfig[1].startTime == '19:00'
    And match response.timeSlotConfig[1].endTime   == '20:00'
    * def sevaMainDaysId = response.id
    * print '✅ Main festival days seva ID:', sevaMainDaysId

  # ── September 19 — Morning only (visarjan day) ──────────────────────────────

  Scenario: Sept 19 — Ganesh Pooja morning only (visarjan closing day)
    Given path '/events/pooja-sevas'
    And request
      """
      {
        "mainEventId":       #(createdEventId),
        "name":              "Ganesh Pooja — Visarjan Morning",
        "type":              "Ganesh Pooja",
        "date":              "2026-09-19",
        "multiDay":          false,
        "duration":          60,
        "slots":             120,
        "needsRegistration": true,
        "isFree":            true,
        "timeSlotConfig": [
          {
            "slotDate":  "2026-09-19",
            "startTime": "10:00",
            "endTime":   "11:00",
            "title":     "Ganesh Pooja — Visarjan Aarti",
            "slotCount": 120
          }
        ]
      }
      """
    When method POST
    Then status 201
    And match response.id                             != null
    And match response.timeSlotConfig[0].startTime    == '10:00'
    And match response.timeSlotConfig[0].endTime      == '11:00'
    And match response.timeSlotConfig[0].slotCount    == 120
    * def seva19MorningId = response.id
    * print '✅ Sept 19 Morning seva ID:', seva19MorningId
