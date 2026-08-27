@ignore
Feature: Create a single user (parameterised helper)
  # Expected call variables: fullName, email, phone, aadharNumber, inviteCode,
  #                          dateOfBirth, gender, flatNo
  # Returns: userId, userToken

  Scenario: Register one devotee account
    Given url baseUrl + '/auth/register'
    And header Content-Type = 'application/json'
    And request
      """
      {
        "fullName":     "#(fullName)",
        "email":        "#(email)",
        "phone":        "#(phone)",
        "password":     "Test@1234",
        "aadharNumber": "#(aadharNumber)",
        "inviteCode":   "#(inviteCode)",
        "dateOfBirth":  "#(dateOfBirth)",
        "gender":       "#(gender)",
        "flatNo":       "#(flatNo)",
        "block":        "A"
      }
      """
    When method POST
    Then status 201
    And match response.token != null
    * def userId    = response.id
    * def userToken = response.token
