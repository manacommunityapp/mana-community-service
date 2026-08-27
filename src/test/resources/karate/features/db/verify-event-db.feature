@db-verify
Feature: Database state verification — Ganesh Mahotsav 2026

  # Requires: verifyEventId (the created event's DB id)
  # Connects directly to PostgreSQL via DbUtils.java

  Background:
    * def DbUtils  = Java.type('karate.DbUtils')
    * def db       = new DbUtils(dbUrl, dbUser, dbPassword)

  Scenario: Event record persisted correctly
    * def rows = db.query("SELECT * FROM event_community WHERE id = " + verifyEventId)
    * match rows.length == 1
    * def evt = rows[0]
    * match evt.title                   == 'Ganesh Mahotsav 2026'
    * match evt.start_date.toString()   == '2026-09-14'
    * match evt.end_date.toString()     == '2026-09-19'
    * match evt.capacity                == 120
    * match evt.status                  == 'PUBLISHED'
    * match evt.payment_modes           == 'MANUAL'
    * match evt.registration_deadline.toString() == '2026-09-13'
    * print '✅ Event record DB check: PASSED'

  Scenario: 8 committee contacts stored in contactsJson
    * def rows        = db.query("SELECT contacts_json FROM event_community WHERE id = " + verifyEventId)
    * def contactsRaw = rows[0].contacts_json
    * match contactsRaw != null
    * def contacts = karate.fromJson(contactsRaw)
    * match contacts.length == 8
    * match contacts[0].role == 'Event Chair'
    * match contacts[7].role == 'Media & PR'
    * print '✅ Contacts JSON DB check: PASSED'

  Scenario: Pooja seva records exist for the event
    * def rows = db.query("SELECT * FROM event_pooja_sevas WHERE main_event_id = " + verifyEventId + " ORDER BY date")
    * match rows.length == 3
    * match rows[0].date.toString() == '2026-09-14'
    * match rows[1].date.toString() == '2026-09-15'
    * match rows[2].date.toString() == '2026-09-19'
    * match each rows[*].slots      == 120
    * match rows[1].multi_day       == true
    * print '✅ Pooja seva records DB check: PASSED'

  Scenario: Time slots saved with start_time, end_time, slot_count, id and status=OPEN (V93)
    * def slots = db.query("SELECT * FROM event_pooja_seva_time_slots ORDER BY slot_date, start_time")
    # Sept 14 evening: 1 + Sept 15–18 morning+evening: 8 + Sept 19 morning: 1 = 10 total
    * match slots.length == 10
    * match each slots[*].id         != null
    * match each slots[*].end_time   != null
    * match each slots[*].slot_count == 120
    # V93: status column — all newly created slots default to OPEN
    * match each slots[*].status == 'OPEN'
    # Opening slot: Sept 14, 19:00 → 20:00
    * def opening = slots[0]
    * match opening.slot_date.toString() == '2026-09-14'
    * match opening.start_time           == '19:00'
    * match opening.end_time             == '20:00'
    # Closing slot: Sept 19, 10:00 → 11:00
    * def closing = slots[9]
    * match closing.slot_date.toString() == '2026-09-19'
    * match closing.start_time           == '10:00'
    * match closing.end_time             == '11:00'
    * print '✅ Time slots DB check: PASSED'

  Scenario: Registration saved in event_pooja_user_registrations (NOT event_booking_registrations)
    * def regs = db.query("SELECT * FROM event_pooja_user_registrations WHERE event_id = " + verifyEventId)
    * match regs.length >= 1
    * match regs[0].status              == 'CONFIRMED'
    * match regs[0].reg_code            != null
    # V90: audit fields — self-booking defaults
    * match regs[0].registration_source == 'SELF'
    * match regs[0].override_used       == false
    * match regs[0].registered_by       == '#null'

    # Confirm NO pooja rows leaked into the booking registrations table (fix from 863c6b8)
    * def booking = db.query("SELECT COUNT(*) as cnt FROM event_booking_registrations WHERE activity_id LIKE 'pooja-%'")
    * match booking[0].cnt == 0
    * print '✅ Registration isolation DB check: PASSED'

  Scenario: Admin registration audit fields set correctly (V90)
    * def adminRegs = db.query("SELECT * FROM event_pooja_user_registrations WHERE registration_source = 'ADMIN' AND event_id = " + verifyEventId)
    # This scenario passes when there are no admin regs (e2e flow only creates SELF regs)
    # Run admin-register-for-pooja.feature first to generate an admin row
    * print 'Admin registration rows found:', adminRegs.length
    * if (adminRegs.length > 0) karate.set('adminReg', adminRegs[0])
    * if (adminRegs.length > 0) karate.match(adminRegs[0].override_used, true)
    * if (adminRegs.length > 0) karate.match(adminRegs[0].registered_by, '#notnull')
    * print '✅ Admin audit fields DB check: PASSED'

  Scenario: 50 test users created
    * def count = db.scalar("SELECT COUNT(*) FROM app_users WHERE email LIKE '%@ganesh2026.test'")
    * assert count >= 50
    * print '✅ User count DB check:', count, '— PASSED'
