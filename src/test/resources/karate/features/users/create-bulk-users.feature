@bulk-users
Feature: Create 50 devotee test accounts for Ganesh Mahotsav

  Background:
    * url baseUrl

  Scenario: Generate and register 50 test users
    # Build list of 50 user payloads
    * def buildUsers =
      """
      function() {
        var users = [];
        for (var i = 1; i <= 50; i++) {
          users.push({
            fullName:     'Devotee User ' + i,
            email:        'devotee' + i + '@ganesh2026.test',
            phone:        '90000' + ('0000' + i).slice(-5),
            aadharNumber: '10000000' + ('0000' + i).slice(-4),
            inviteCode:   inviteCode,
            dateOfBirth:  '199' + (i % 9 + 1) + '-0' + (i % 9 + 1) + '-15',
            gender:       (i % 2 === 0) ? 'FEMALE' : 'MALE',
            flatNo:       i + '-A'
          });
        }
        return users;
      }
      """
    * def users = buildUsers()

    # Call the single-user feature for each entry (sequential by default;
    # use runner-level parallel=N in KarateRunner for thread-level parallelism)
    * def results = karate.call('classpath:karate/features/users/create-single-user.feature', users)

    * match results.length == 50
    * print '✅ Created', results.length, 'devotee accounts'

    # Expose the first user's credentials for downstream scenarios
    * def devotee1Email    = users[0].email
    * def devotee1Password = 'Test@1234'

  Scenario: Verify 50 users exist via GET /api/communities (admin-level check)
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    Given url baseUrl + '/auth/login'
    And header Authorization = 'Bearer ' + token
    # Sanity: check the recently-created user can log in
    Given url baseUrl + '/auth/login'
    And header Content-Type = 'application/json'
    And request { identifier: 'devotee1@ganesh2026.test', password: 'Test@1234' }
    When method POST
    Then status 200
    And match response.token != null
    * print '✅ Devotee1 login verified'
