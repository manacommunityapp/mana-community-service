@event-create @smoke
Feature: Create Ganesh Mahotsav 2026 — multi-day community event

  Background:
    * url baseUrl
    * def auth  = callonce read('classpath:karate/features/auth/login.feature')
    * def token = auth.authToken
    * header Authorization  = 'Bearer ' + token
    * header Content-Type   = 'application/json'
    * header X-Community-Id = communityId

  Scenario: Create the main Ganesh Mahotsav event (Sept 14–19)
    # Build contacts array for 8 committee members
    * def contacts =
      """
      [
        { "name": "Ramesh Kumar",  "role": "Event Chair",        "phone": "9000000001", "email": "ramesh@community.org"  },
        { "name": "Priya Sharma",  "role": "Co-Chair",            "phone": "9000000002", "email": "priya@community.org"   },
        { "name": "Suresh Patel",  "role": "Pooja Coordinator",   "phone": "9000000003", "email": "suresh@community.org"  },
        { "name": "Anita Reddy",   "role": "Cultural Head",       "phone": "9000000004", "email": "anita@community.org"   },
        { "name": "Vijay Nair",    "role": "Volunteer Lead",      "phone": "9000000005", "email": "vijay@community.org"   },
        { "name": "Deepa Iyer",    "role": "Finance",             "phone": "9000000006", "email": "deepa@community.org"   },
        { "name": "Arun Menon",    "role": "Logistics",           "phone": "9000000007", "email": "arun@community.org"    },
        { "name": "Kavitha Rao",   "role": "Media & PR",          "phone": "9000000008", "email": "kavitha@community.org" }
      ]
      """
    * def contactsJsonStr = karate.toJson(contacts)

    Given path '/events'
    And request
      """
      {
        "title":                "Ganesh Mahotsav 2026",
        "description":          "Annual Ganesh festival featuring devotional pooja services, cultural programs and community gatherings. All residents are welcome.",
        "type":                 "CULTURAL",
        "startDate":            "2026-09-14",
        "endDate":              "2026-09-19",
        "locationType":         "IN_PERSON",
        "location":             "Community Maidan & Mandap",
        "city":                 "Hyderabad",
        "priceType":            "PAID",
        "paymentModes":         "MANUAL",
        "paymentInstructions":  "Pay by cash at the registration desk. Receipts issued for all payments. Contact the finance committee for bank transfer details.",
        "capacity":             120,
        "maxAttendees":         120,
        "registrationDeadline": "2026-09-13",
        "category":             "GENERAL",
        "status":               "PUBLISHED",
        "notes":                "Dress code: traditional attire preferred. No footwear inside the mandap. Prasadam distributed after each pooja.",
        "contactsJson":         "#(contactsJsonStr)"
      }
      """
    When method POST
    Then status 201
    And match response.id          != null
    And match response.title        == 'Ganesh Mahotsav 2026'
    And match response.startDate    == '2026-09-14'
    And match response.endDate      == '2026-09-19'
    And match response.capacity     == 120
    And match response.status       == 'PUBLISHED'
    And match response.paymentModes == 'MANUAL'

    * def eventId = response.id
    * print '✅ Event created with ID:', eventId
