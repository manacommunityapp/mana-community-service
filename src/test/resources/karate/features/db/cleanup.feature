@cleanup
Feature: Test data cleanup — wipe all Karate-generated data before and after runs

  # Deletes in FK-safe order (children before parents).
  # Safe on an empty DB — DELETE of 0 rows is never an error.
  # Table name: app_user (singular) as per @Table(name="app_user") JPA mapping.
  # Called from KarateRunner @BeforeAll and @AfterAll.

  Background:
    * def DbUtils = Java.type('karate.DbUtils')
    * def db      = new DbUtils(dbUrl, dbUser, dbPassword)

  # ═══════════════════════════════════════════════════════════
  # 1. EVENT MODULE — Ganesh Mahotsav test data
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe event module test data (event, sevas, slots, registrations)
    # Pooja registrations for all Ganesh Mahotsav events
    * def r1 = db.execute("DELETE FROM event_pooja_user_registrations WHERE event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026')")
    * print '  [cleanup] pooja registrations deleted:', r1

    # Time slots for those sevas
    * def r2 = db.execute("DELETE FROM event_pooja_seva_time_slots WHERE pooja_seva_id IN (SELECT id FROM event_pooja_sevas WHERE main_event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026'))")
    * print '  [cleanup] time slots deleted:', r2

    # Pooja sevas
    * def r3 = db.execute("DELETE FROM event_pooja_sevas WHERE main_event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026')")
    * print '  [cleanup] pooja sevas deleted:', r3

    # Parent event record
    * def r4 = db.execute("DELETE FROM event_community WHERE title = 'Ganesh Mahotsav 2026'")
    * print '  [cleanup] events deleted:', r4

    * print '✅ Event module cleanup complete'

  # ═══════════════════════════════════════════════════════════
  # 2. BULK DEVOTEE USERS — devotee1-250@ganesh2026.test
  #    UNIQUE(email) constraint blocks re-run without this.
  #    Delete FK child rows first; app_user_roles cascades.
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe bulk devotee accounts and their FK child rows
    # Pooja registrations (any event, not only Ganesh Mahotsav)
    * def r5 = db.execute("DELETE FROM event_pooja_user_registrations WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@ganesh2026.test')")
    * print '  [cleanup] pooja regs for devotees deleted:', r5

    # Booking registrations (sports, cultural, etc.)
    * def r6 = db.execute("DELETE FROM event_booking_registrations WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@ganesh2026.test')")
    * print '  [cleanup] booking regs for devotees deleted:', r6

    # User sessions (no FK constraint, just indexed — safe to delete)
    * def r7 = db.execute("DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@ganesh2026.test')")
    * print '  [cleanup] sessions for devotees deleted:', r7

    # app_user_roles has ON DELETE CASCADE so this is belt-and-suspenders
    * def r8 = db.execute("DELETE FROM app_user_roles WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@ganesh2026.test')")
    * print '  [cleanup] roles for devotees deleted:', r8

    # Now safe to delete the users
    * def r9 = db.execute("DELETE FROM app_user WHERE email LIKE '%@ganesh2026.test'")
    * print '  [cleanup] devotee accounts deleted:', r9

    * print '✅ Devotee account cleanup complete'

  # ═══════════════════════════════════════════════════════════
  # 3. FEED TEST USERS — feeduser_*@feed.test
  #    post.user_id has no FK constraint so users can be deleted
  #    independently, but clean up posts first for a tidy DB.
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe feed test users and their posts
    # comment child rows (FK: post_comment_id → post_comment.id)
    * def f1 = db.execute("DELETE FROM post_comment_like WHERE comment_id IN (SELECT id FROM post_comment WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test'))")
    * def f2 = db.execute("DELETE FROM post_comment_reaction WHERE comment_id IN (SELECT id FROM post_comment WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test'))")
    * def f3 = db.execute("DELETE FROM post_comment WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")

    # post child rows (FK: post_id → post.id)
    * def f4 = db.execute("DELETE FROM post_hashtag WHERE post_id IN (SELECT id FROM post WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test'))")
    * def f5 = db.execute("DELETE FROM post_media WHERE post_id IN (SELECT id FROM post WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test'))")
    * def f6 = db.execute("DELETE FROM post_like WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")
    * def f7 = db.execute("DELETE FROM post_reaction WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")
    * def f8 = db.execute("DELETE FROM post_bookmark WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")
    * def f9 = db.execute("DELETE FROM post_poll_vote WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")

    # posts themselves
    * def f10 = db.execute("DELETE FROM post WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")

    # sessions
    * def f11 = db.execute("DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test')")

    # users
    * def f12 = db.execute("DELETE FROM app_user WHERE email LIKE 'feeduser\\_%@feed.test'")
    * print '  [cleanup] feed posts/comments deleted:', (f3 + f10), '| feed users deleted:', f12

    * print '✅ Feed test user cleanup complete'

  # ═══════════════════════════════════════════════════════════
  # 4. SIGNUP / GENERAL TEST USERS
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe accumulated signup test accounts
    * def s1 = db.execute("DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@signup.test' OR email LIKE 'signup\\_%' OR email LIKE 'testuser\\_%')")
    * def s2 = db.execute("DELETE FROM app_user WHERE email LIKE '%@signup.test' OR email LIKE 'signup\\_%@%' OR email LIKE 'testuser\\_%@%'")
    * print '  [cleanup] signup test accounts deleted:', s2

    * print '✅ Signup account cleanup complete'
