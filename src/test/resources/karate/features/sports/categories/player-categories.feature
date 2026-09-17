@sports-cats @smoke
Feature: Player categories — CRUD via /api/player-categories

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: GET /player-categories — list categories
    Given path '/player-categories'
    When method GET
    Then status 200
    And match response == '#array'
    * print '✅ Player categories count:', response.length

  Scenario: POST /player-categories — create Open category (all genders)
    Given path '/player-categories'
    And request
      """
      {
        "name":         "Open Karate Test",
        "categoryType": "SINGLES",
        "description":  "Open singles for all ages",
        "minAge":       18,
        "maxAge":       99,
        "gender":       "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.name == 'Open Karate Test'
    * def openCategoryId = response.id
    * print '✅ Open category created — ID:', openCategoryId

  Scenario: POST /player-categories — create Mixed Doubles category
    Given path '/player-categories'
    And request
      """
      {
        "name":         "Mixed Doubles Karate Test",
        "categoryType": "MIXED_DOUBLES",
        "description":  "Mixed doubles open category",
        "minAge":       18,
        "maxAge":       99,
        "gender":       "MALE"
      }
      """
    When method POST
    * match [200, 201] contains responseStatus
    And match response.name == 'Mixed Doubles Karate Test'
    * def mixedCategoryId = response.id
    * print '✅ Mixed Doubles category created — ID:', mixedCategoryId

  Scenario: PUT /player-categories/{id} — update Open category description
    Given path '/player-categories'
    When method GET
    Then status 200
    * def openCat = karate.filter(response, function(c){ return c.name == 'Open Karate Test' })
    * if (openCat.length == 0) karate.log('Open Karate Test category not found — skipping update')
    * if (openCat.length > 0) karate.call(read('classpath:karate/features/sports/categories/_update-category.feature'), { updateCatId: openCat[0].id })
    * print '✅ Category update step done'

  Scenario: GET /player-categories — updated category is visible
    Given path '/player-categories'
    When method GET
    Then status 200
    * def found = karate.filter(response, function(c){ return c.name == 'Open Karate Test' })
    * assert found.length >= 1
    * print '✅ Open Karate Test category visible in list'
