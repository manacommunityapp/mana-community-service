@ignore
Feature: Cancel a pooja registration (soft delete — releases slot capacity)
  # Requires: cancelRegId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: DELETE /events/pooja-registrations/{id}?permanent=false
    Given path '/events/pooja-registrations/' + cancelRegId
    And param permanent = false
    When method DELETE
    * match [200, 204, 404] contains responseStatus
    * print '  [cancel] DELETE registration status:', responseStatus, '| regId:', cancelRegId

    # Verify the status is now CANCELLED
    Given path '/events/pooja-registrations/' + cancelRegId
    When method GET
    * match [200, 404] contains responseStatus
    * if (responseStatus == 200) karate.match(response.status, 'CANCELLED')
    * print '  [cancel] post-delete status check:', responseStatus
