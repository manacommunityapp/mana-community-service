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
    # ── FK order: participants → registrations → schedule-reservations → schedules → time-slots → sevas → event

    # 1. Booking participants (FK → registrations)
    * def r0 = db.execute("DELETE FROM event_pooja_booking_participants WHERE registration_id IN (SELECT id FROM event_pooja_user_registrations WHERE event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026'))")
    * print '  [cleanup] booking participants deleted:', r0

    # 2. Pooja registrations
    * def r1 = db.execute("DELETE FROM event_pooja_user_registrations WHERE event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026')")
    * print '  [cleanup] pooja registrations deleted:', r1

    # 3. Schedule reservations (FK → schedules)
    * def r1b = db.execute("DELETE FROM event_pooja_schedule_reservations WHERE schedule_id IN (SELECT id FROM event_pooja_schedules WHERE pooja_id IN (SELECT id FROM event_pooja_sevas WHERE main_event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026')))")
    * print '  [cleanup] schedule reservations deleted:', r1b

    # 4. Admin-created pooja schedules (FK → sevas)
    * def r1c = db.execute("DELETE FROM event_pooja_schedules WHERE pooja_id IN (SELECT id FROM event_pooja_sevas WHERE main_event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026'))")
    * print '  [cleanup] pooja schedules deleted:', r1c

    # 5. Time slots for those sevas
    * def r2 = db.execute("DELETE FROM event_pooja_seva_time_slots WHERE pooja_seva_id IN (SELECT id FROM event_pooja_sevas WHERE main_event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026'))")
    * print '  [cleanup] time slots deleted:', r2

    # 6. Pooja sevas
    * def r3 = db.execute("DELETE FROM event_pooja_sevas WHERE main_event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026')")
    * print '  [cleanup] pooja sevas deleted:', r3

    # 7. Event registrations (general, non-pooja)
    * def r3b = db.execute("DELETE FROM event_booking_registrations WHERE event_id IN (SELECT id FROM event_community WHERE title = 'Ganesh Mahotsav 2026')")
    * print '  [cleanup] general event registrations deleted:', r3b

    # 8. Parent event record
    * def r4 = db.execute("DELETE FROM event_community WHERE title = 'Ganesh Mahotsav 2026'")
    * print '  [cleanup] events deleted:', r4

    * print '✅ Event module cleanup complete'

  # ═══════════════════════════════════════════════════════════
  # 2. BULK DEVOTEE USERS — devotee1-250@ganesh2026.test
  #    UNIQUE(email) constraint blocks re-run without this.
  #    Delete FK child rows first; app_user_roles cascades.
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe bulk devotee accounts and their FK child rows
    # Pooja booking participants
    * def r4b = db.execute("DELETE FROM event_pooja_booking_participants WHERE registration_id IN (SELECT id FROM event_pooja_user_registrations WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@ganesh2026.test'))")
    * print '  [cleanup] booking participants for devotees deleted:', r4b

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
  # 4. SPORTS MODULE — Karate test data
  #    Naming sentinel: all Karate-created sports data uses
  #    "Karate Test" in name fields for safe targeted cleanup.
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe sports module Karate test data
    # ── Auction module (deepest FKs first) ────────────────
    * def sa1 = db.execute("DELETE FROM sports_auction_bid WHERE config_id IN (SELECT id FROM sports_auction_config WHERE season_name LIKE '%Karate Test%')")
    * print '  [cleanup] sports auction bids deleted:', sa1

    * def sa2 = db.execute("DELETE FROM sports_auction_player WHERE config_id IN (SELECT id FROM sports_auction_config WHERE season_name LIKE '%Karate Test%')")
    * print '  [cleanup] sports auction players deleted:', sa2

    * def sa3 = db.execute("DELETE FROM sports_auction_team WHERE config_id IN (SELECT id FROM sports_auction_config WHERE season_name LIKE '%Karate Test%')")
    * print '  [cleanup] sports auction teams deleted:', sa3

    * def sa4 = db.execute("DELETE FROM sports_auction_config WHERE season_name LIKE '%Karate Test%'")
    * print '  [cleanup] sports auction configs deleted:', sa4

    # ── Tournament scheduler (matches, groups, standings, config) ──
    * def st1 = db.execute("DELETE FROM sports_match_ball_event WHERE match_id IN (SELECT id FROM sports_tournament_match WHERE community_id = " + communityId + " AND home_team_id IN (SELECT id FROM sports_auction_team WHERE team_name LIKE '%Karate Test%'))")
    * print '  [cleanup] ball events deleted:', st1

    * def st2 = db.execute("DELETE FROM sports_match_result WHERE match_id IN (SELECT id FROM sports_tournament_match WHERE id IN (SELECT match_id FROM sports_tournament_group WHERE config_id IN (SELECT id FROM sports_tournament_config WHERE tournament_name LIKE '%Karate Test%')))")
    * print '  [cleanup] match results deleted:', st2

    * def st3 = db.execute("DELETE FROM sports_tournament_match WHERE id IN (SELECT match_id FROM sports_tournament_group WHERE config_id IN (SELECT id FROM sports_tournament_config WHERE tournament_name LIKE '%Karate Test%'))")
    * print '  [cleanup] tournament matches deleted:', st3

    * def st4 = db.execute("DELETE FROM sports_group_team_standing WHERE group_id IN (SELECT id FROM sports_tournament_group WHERE config_id IN (SELECT id FROM sports_tournament_config WHERE tournament_name LIKE '%Karate Test%'))")
    * print '  [cleanup] group standings deleted:', st4

    * def st5 = db.execute("DELETE FROM sports_tournament_group WHERE config_id IN (SELECT id FROM sports_tournament_config WHERE tournament_name LIKE '%Karate Test%')")
    * print '  [cleanup] tournament groups deleted:', st5

    * def st6 = db.execute("DELETE FROM sports_schedule_generation_log WHERE config_id IN (SELECT id FROM sports_tournament_config WHERE tournament_name LIKE '%Karate Test%')")
    * print '  [cleanup] schedule generation logs deleted:', st6

    * def st7 = db.execute("DELETE FROM sports_tournament_config WHERE tournament_name LIKE '%Karate Test%'")
    * print '  [cleanup] tournament scheduler configs deleted:', st7

    # ── Tournament content and top-level tournaments ───────
    * def sc1 = db.execute("DELETE FROM sports_tournament_announcement WHERE tournament_id IN (SELECT id FROM sports_tournament WHERE name LIKE '%Karate Test%')")
    * print '  [cleanup] tournament announcements deleted:', sc1

    * def sc2 = db.execute("DELETE FROM sports_tournament_gallery_image WHERE tournament_id IN (SELECT id FROM sports_tournament WHERE name LIKE '%Karate Test%')")
    * print '  [cleanup] tournament gallery images deleted:', sc2

    * def sc3 = db.execute("DELETE FROM sports_tournament_timeline_entry WHERE tournament_id IN (SELECT id FROM sports_tournament WHERE name LIKE '%Karate Test%')")
    * print '  [cleanup] tournament timeline entries deleted:', sc3

    * def sc4 = db.execute("DELETE FROM sports_tournament WHERE name LIKE '%Karate Test%'")
    * print '  [cleanup] tournaments deleted:', sc4

    # ── Sports event registrations ─────────────────────────
    * def sr1 = db.execute("DELETE FROM sports_event_registration WHERE event_id IN (SELECT id FROM sports_event WHERE name LIKE '%Karate Test%')")
    * print '  [cleanup] sports registrations deleted:', sr1

    # ── Sports events ──────────────────────────────────────
    * def se1 = db.execute("DELETE FROM sports_event WHERE name LIKE '%Karate Test%'")
    * print '  [cleanup] sports events deleted:', se1

    # ── Player categories ──────────────────────────────────
    * def spc = db.execute("DELETE FROM sports_player_category WHERE name LIKE '%Karate Test%'")
    * print '  [cleanup] player categories deleted:', spc

    # ── Sports meta (soft-delete already done, hard-delete here for test isolation) ──
    * def sm1 = db.execute("DELETE FROM sports_meta WHERE name LIKE '%Karate Test%'")
    * print '  [cleanup] sports meta entries deleted:', sm1

    # ── Sports test users (@sports.test) ───────────────────
    * def su1 = db.execute("DELETE FROM sports_event_registration WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@sports.test')")
    * print '  [cleanup] sports user registrations deleted:', su1

    * def su2 = db.execute("DELETE FROM sports_player_ranking WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@sports.test')")
    * print '  [cleanup] sports user rankings deleted:', su2

    * def su3 = db.execute("DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@sports.test')")
    * print '  [cleanup] sports user sessions deleted:', su3

    * def su4 = db.execute("DELETE FROM app_user WHERE email LIKE '%@sports.test'")
    * print '  [cleanup] sports test users deleted:', su4

    * print '✅ Sports module cleanup complete'

  # ═══════════════════════════════════════════════════════════
  # 5. SIGNUP / GENERAL TEST USERS
  # ═══════════════════════════════════════════════════════════
  Scenario: Wipe accumulated signup test accounts
    * def s1 = db.execute("DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@signup.test' OR email LIKE 'signup\\_%' OR email LIKE 'testuser\\_%')")
    * def s2 = db.execute("DELETE FROM app_user WHERE email LIKE '%@signup.test' OR email LIKE 'signup\\_%@%' OR email LIKE 'testuser\\_%@%'")
    * print '  [cleanup] signup test accounts deleted:', s2

    * print '✅ Signup account cleanup complete'
