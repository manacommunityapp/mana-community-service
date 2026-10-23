@ignore
Feature: Reserve a slot and create a pooja registration (internal helper)
  # Requires: doScheduleId, doSlotId, doEventId, doIdem, doDate, doTime, doName

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Reserve then register
    Given path '/events/pooja-schedules/' + doScheduleId + '/reserve'
    And request { idempotencyKey: '#(doIdem)', familyCount: 1, devoteeCount: 1 }
    When method POST
    Then status 200
    * def doResId = response.reservationId

    Given path '/events/pooja-registrations'
    And request
      """
      {
        "eventId":              #(doEventId),
        "poojaSevaTimeSlotsId": #(doSlotId),
        "scheduleId":           #(doScheduleId),
        "reservationId":        #(doResId),
        "participantName":      "#(doName)",
        "devoteeCount":         1,
        "poojaSlotDate":        "#(doDate)",
        "poojaSlotTime":        "#(doTime)",
        "paymentMethod":        "MANUAL",
        "paymentStatus":        "PENDING"
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * def helperRegId = (responseStatus == 409) ? null : response.id
    * print '  [helper] reserve-and-register status:', responseStatus, '| regId:', helperRegId
