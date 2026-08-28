@ignore
Feature: Create a single user (parameterised helper)
  # Expected call variables: fullName, email, phone, aadharNumber, inviteCode,
  #                          dateOfBirth, gender, flatNo
  # Returns: userId, userToken

  Scenario: Register one devotee account (idempotent — 409 = already exists, skip)
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
    # 201 = created fresh; 409 = user already exists from a prior run — both are acceptable
    * match [201, 409] contains responseStatus
    * def created   = responseStatus == 201
    * def userId    = created ? response.id    : null
    * def userToken = created ? response.token : null
    * if (!created) karate.log('User already exists, skipping:', email)
