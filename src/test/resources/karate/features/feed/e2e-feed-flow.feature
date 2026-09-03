@feed @e2e @smoke
Feature: End-to-end Community Feed flow

  # Orchestrates: register user → post → comment → like → bookmark → search → verify

  Scenario: Full community feed lifecycle
    # 1. Register a fresh user for this flow
    * def ts        = java.lang.System.currentTimeMillis()
    * def feedEmail = 'feeduser_' + ts + '@feed.test'
    * def feedPhone = '600' + (ts + '').slice(-8)

    Given url baseUrl + '/auth/register'
    And header Content-Type = 'application/json'
    And request
      """
      {
        "fullName":     "Feed Test User",
        "email":        "#(feedEmail)",
        "phone":        "#(feedPhone)",
        "password":     "Test@1234",
        "aadharNumber": "555500000001",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "1993-04-12",
        "gender":       "FEMALE",
        "flatNo":       "505-E"
      }
      """
    When method POST
    Then status 201
    * def feedToken = response.token
    * print '✅ Step 1 — Feed user registered'

    # 2. Create a community post
    Given url baseUrl + '/posts'
    And header Authorization = 'Bearer ' + feedToken
    And header Content-Type  = 'application/json'
    And request
      """
      {
        "content":    "Jai Ganesh! 🪔 Happy to join the Mana Community. Excited for Mahotsav!",
        "type":       "TEXT",
        "visibility": "COMMUNITY"
      }
      """
    When method POST
    Then status 200
    * def postId = response.id
    * print '✅ Step 2 — Post created ID:', postId

    # 3. Add a comment
    Given url baseUrl + '/posts/' + postId + '/comments'
    And header Authorization = 'Bearer ' + feedToken
    And header Content-Type  = 'application/json'
    And request { content: 'Looking forward to the evening pooja! 🙏' }
    When method POST
    Then status 200
    * def commentId = response.id
    * print '✅ Step 3 — Comment added ID:', commentId

    # 4. Like the comment
    Given url baseUrl + '/posts/comments/' + commentId + '/like'
    And header Authorization = 'Bearer ' + feedToken
    When method POST
    Then status 200
    And match response.liked == true
    * print '✅ Step 4 — Comment liked'

    # 5. React to the post with CELEBRATE
    Given url baseUrl + '/posts/' + postId + '/react'
    And header Authorization = 'Bearer ' + feedToken
    And header Content-Type  = 'application/json'
    And request { reactionType: 'CELEBRATE' }
    When method POST
    Then status 200
    * print '✅ Step 5 — Post reaction added'

    # 6. Bookmark the post
    Given url baseUrl + '/posts/' + postId + '/bookmark'
    And header Authorization = 'Bearer ' + feedToken
    When method POST
    Then status 200
    * print '✅ Step 6 — Post bookmarked'

    # 7. Confirm it appears in bookmarks
    Given url baseUrl + '/posts/bookmarks'
    And header Authorization = 'Bearer ' + feedToken
    And param page = 0
    And param size = 10
    When method GET
    Then status 200
    * assert response.content.length >= 1
    * def found = karate.filter(response.content, function(p){ return p.id == postId })
    * assert found.length >= 1
    * print '✅ Step 7 — Post appears in bookmarks'

    # 8. Fetch feed to confirm post is visible
    Given url baseUrl + '/posts'
    And header Authorization = 'Bearer ' + feedToken
    And param page = 0
    And param size = 20
    When method GET
    Then status 200
    And match response.content != null
    * print '✅ Step 8 — Feed fetched, totalElements:', response.totalElements

    * print ''
    * print '═══════════════════════════════════════'
    * print '✅  E2E COMMUNITY FEED FLOW PASSED'
    * print '   Post ID    :', postId
    * print '   Comment ID :', commentId
    * print '═══════════════════════════════════════'
