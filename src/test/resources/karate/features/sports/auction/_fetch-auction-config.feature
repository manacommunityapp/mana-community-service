@ignore
Feature: Fetch single auction config
  # Requires: fetchAuctionConfigId

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header X-Community-Id = communityId

  Scenario: GET /auction/config/{id}
    Given path '/auction/config/' + fetchAuctionConfigId
    When method GET
    Then status 200
    And match response.id == fetchAuctionConfigId
    * print '  [fetch-auction-config] seasonName:', response.seasonName
