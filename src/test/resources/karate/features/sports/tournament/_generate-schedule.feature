@ignore
Feature: Generate a tournament schedule (stateless preview)
  # Requires: genConfigId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: POST /tournament/schedule — stateless generation
    Given path '/tournament/schedule'
    And request { configId: '#(genConfigId)' }
    When method POST
    * match [200, 201, 400] contains responseStatus
    * print '  [generate-schedule] status:', responseStatus, '| configId:', genConfigId
