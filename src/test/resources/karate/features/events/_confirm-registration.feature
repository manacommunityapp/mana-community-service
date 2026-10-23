@ignore
Feature: Confirm a single event registration
  # Requires: confirmRegId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /events/registrations/{regId}/confirm
    Given path '/events/registrations/' + confirmRegId + '/confirm'
    When method PUT
    * match [200, 201, 204, 409] contains responseStatus
    * print '✅ Confirm registration status:', responseStatus
