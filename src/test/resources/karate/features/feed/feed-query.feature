@feed @smoke
Feature: Community Feed — read, search, and bookmarks

  # Covers: GET /api/posts, GET /api/posts/search, GET /api/posts/bookmarks,
  #         GET /api/posts/summary-counts

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization = 'Bearer ' + token
    * header Content-Type  = 'application/json'

  Scenario: Get paginated feed (first page)
    Given path '/posts'
    And param page = 0
    And param size = 10
    When method GET
    Then status 200
    And match response.content       != null
    And match response.totalElements >= 0
    * print '✅ Feed page 0 — total:', response.totalElements

  Scenario: Get feed filtered by type ANNOUNCEMENT
    Given path '/posts'
    And param type = 'ANNOUNCEMENT'
    And param page = 0
    And param size = 10
    When method GET
    Then status 200
    And match response.content != null
    * print '✅ Announcements feed count:', response.content.length

  Scenario: Search posts by keyword
    # Seed a searchable post first
    Given path '/posts'
    And request { content: 'Karate search keyword unique_xyz_2026', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200

    Given path '/posts/search'
    And param q    = 'unique_xyz_2026'
    And param page = 0
    And param size = 5
    When method GET
    Then status 200
    And match response.content.length >= 1
    * print '✅ Search results:', response.content.length

  Scenario: Get bookmarked posts
    # Seed and bookmark a post
    Given path '/posts'
    And request { content: 'Bookmark feed test post.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def bkPostId = response.id

    Given path '/posts/' + bkPostId + '/bookmark'
    When method POST
    Then status 200

    Given path '/posts/bookmarks'
    And param page = 0
    And param size = 10
    When method GET
    Then status 200
    And match response.content.length >= 1
    * print '✅ Bookmarks count:', response.content.length

  Scenario: Get sidebar summary counts
    Given path '/posts/summary-counts'
    When method GET
    Then status 200
    And match response != null
    * print '✅ Summary counts:', response
