@feed @comments @smoke
Feature: Community Feed — comments, comment likes, and reactions

  # Covers: POST /api/posts/{id}/comments, GET /api/posts/{id}/comments,
  #         POST /api/posts/comments/{id}/like, DELETE /api/posts/comments/{id}
  #         POST /api/posts/comments/{id}/react

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'

  Scenario: Add a comment to a post
    # Create parent post
    Given path '/posts'
    And request { content: 'Post for commenting.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def commentParentId = response.id

    Given path '/posts/' + commentParentId + '/comments'
    And request { content: 'Great post! Jai Ganesh! 🙏' }
    When method POST
    Then status 200
    And match response.id      != null
    And match response.content == 'Great post! Jai Ganesh! 🙏'
    * def commentId = response.id
    * print '✅ Comment added — ID:', commentId

  Scenario: Get all comments on a post
    Given path '/posts'
    And request { content: 'Post with comments.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def fetchPostId = response.id

    # Add two comments
    Given path '/posts/' + fetchPostId + '/comments'
    And request { content: 'First comment' }
    When method POST
    Then status 200

    Given path '/posts/' + fetchPostId + '/comments'
    And request { content: 'Second comment' }
    When method POST
    Then status 200

    Given path '/posts/' + fetchPostId + '/comments'
    When method GET
    Then status 200
    And match response.length >= 2
    * print '✅ Comments fetched:', response.length

  Scenario: Like a comment
    Given path '/posts'
    And request { content: 'Post for comment like.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def likePostId = response.id

    Given path '/posts/' + likePostId + '/comments'
    And request { content: 'Like this comment' }
    When method POST
    Then status 200
    * def likeCommentId = response.id

    Given path '/posts/comments/' + likeCommentId + '/like'
    When method POST
    Then status 200
    And match response.liked == true
    * print '✅ Comment liked — ID:', likeCommentId

  Scenario: React to a comment with HELPFUL
    Given path '/posts'
    And request { content: 'Post for comment react.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def reactParentId = response.id

    Given path '/posts/' + reactParentId + '/comments'
    And request { content: 'React to this comment' }
    When method POST
    Then status 200
    * def reactCommentId = response.id

    Given path '/posts/comments/' + reactCommentId + '/react'
    And param type = 'HELPFUL'
    When method POST
    Then status 200
    * print '✅ Comment reaction set — ID:', reactCommentId

  Scenario: Delete a comment
    Given path '/posts'
    And request { content: 'Post for comment delete.', type: 'TEXT', visibility: 'COMMUNITY' }
    When method POST
    Then status 200
    * def deleteParentId = response.id

    Given path '/posts/' + deleteParentId + '/comments'
    And request { content: 'Delete me' }
    When method POST
    Then status 200
    * def deleteCommentId = response.id

    Given path '/posts/comments/' + deleteCommentId
    When method DELETE
    Then status 204
    * print '✅ Comment deleted — ID:', deleteCommentId
