@ignore
Feature: Reschedule one pooja registration (internal helper)
  # Requires: rsRegId, rsNewScheduleId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /events/pooja-registrations/{id}/reschedule
    Given path '/events/pooja-registrations/' + rsRegId + '/reschedule'
    And request
      """
      {
        "newScheduleId":  #(rsNewScheduleId),
        "idempotencyKey": "#(java.util.UUID.randomUUID().toString())"
      }
      """
    When method POST
    * match [200, 201, 409] contains responseStatus
    * print '  [reschedule] status:', responseStatus, '| regId:', rsRegId, '→ scheduleId:', rsNewScheduleId
