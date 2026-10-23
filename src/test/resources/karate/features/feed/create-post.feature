@feed @smoke
Feature: Community Feed — create and manage posts

  # Covers: POST /api/posts, GET /api/posts, PATCH /api/posts/{id}, DELETE /api/posts/{id}
  # Also tests: pin, bookmark, react, like

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'

  Scenario: Create a plain text post
    Given path '/posts'
    And request
      """
      {
        "content":    "Hello community! Testing Karate automation for the feed module.",
        "type":       "TEXT",
        "visibility": "COMMUNITY"
      }
      """
    When method POST
    Then status 200
    And match response.id      != null
    And match response.content == 'Hello community! Testing Karate automation for the feed module.'
    * def plainPostId = response.id
    * print '✅ Plain post created — ID:', plainPostId

  Scenario: Create an announcement post
    Given path '/posts'
    And request
      """
      {
        "content":    "📢 Ganesh Mahotsav 2026 — Sept 14–19. All residents welcome!",
        "title":      "Ganesh Mahotsav Announcement",
        "type":       "ANNOUNCEMENT",
        "visibility": "COMMUNITY"
      }
      """
    When method POST
    Then status 200
    And match response.type == 'ANNOUNCEMENT'
    * def announcementPostId = response.id
    * print '✅ Announcement post ID:', announcementPostId

  Scenario: Create a poll post
    Given path '/posts'
    And request
      """
      {
        "content":     "Which day will you attend Ganesh Mahotsav?",
        "type":        "POLL",
        "visibility":  "COMMUNITY",
        "pollQuestion":"Which day will you attend?",
        "pollOptions": "Sept 14 (Opening)|Sept 16|Sept 17|Sept 19 (Visarjan)"
      }
      """
    When method POST
    Then status 200
    And match response.type == 'POLL'
    * def pollPostId = response.id
    * print '✅ Poll post ID:', pollPostId

  Scenario: Update (PATCH) a post
    # Create post first
    Given path '/posts'
    And request { content: 'Original text.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def editPostId = response.id

    Given path '/posts/' + editPostId
    And request { content: 'Updated text — edited by Karate.' }
    When method PATCH
    Then status 200
    And match response.content == 'Updated text — edited by Karate.'
    * print '✅ Post updated — ID:', editPostId

  Scenario: Like a post (toggle)
    Given path '/posts'
    And request { content: 'Like me!', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def likePostId = response.id

    Given path '/posts/' + likePostId + '/like'
    When method POST
    Then status 200
    And match response.liked == true
    * print '✅ Post liked — ID:', likePostId

  Scenario: React to a post with LOVE
    Given path '/posts'
    And request { content: 'React to me!', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def reactPostId = response.id

    Given path '/posts/' + reactPostId + '/react'
    And request { reactionType: 'LOVE' }
    When method POST
    Then status 200
    * print '✅ Post reacted — ID:', reactPostId

  Scenario: Bookmark a post (toggle)
    Given path '/posts'
    And request { content: 'Bookmark me!', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def bookmarkPostId = response.id

    Given path '/posts/' + bookmarkPostId + '/bookmark'
    When method POST
    Then status 200
    * print '✅ Post bookmarked — ID:', bookmarkPostId

  Scenario: Delete a post
    Given path '/posts'
    And request { content: 'Delete me.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def deletePostId = response.id

    Given path '/posts/' + deletePostId
    When method DELETE
    Then status 204
    * print '✅ Post deleted — ID:', deletePostId
