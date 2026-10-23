@ignore
Feature: Accept a partner invitation
  # Requires: acceptRegId, userToken

  Background:
    * url baseUrl
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: PUT /sports/registrations/{id}/partner-confirm?accept=true
    Given path '/sports/registrations/' + acceptRegId + '/partner-confirm'
    And header Authorization = 'Bearer ' + userToken
    And param accept = true
    When method PUT
    * match [200, 201, 204] contains responseStatus
    * print '  [accept-invite] status:', responseStatus, '| regId:', acceptRegId
