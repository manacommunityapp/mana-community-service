@sports-db-verify
Feature: Database state verification — Sports module Karate tests

  # Requires: verifySportsEventId (the cricket Karate Test event DB id)
  # Connects directly to PostgreSQL via DbUtils.java

  Background:
    * def DbUtils = Java.type('karate.DbUtils')
    * def db      = new DbUtils(dbUrl, dbUser, dbPassword)

  Scenario: Sports event record persisted correctly
    * def rows = db.query("SELECT * FROM sports_event WHERE id = " + verifySportsEventId)
    * match rows.length == 1
    * def evt = rows[0]
    * match evt.name == 'Cricket Karate Test Tournament 2026'
    * print '✅ Sports event DB check — PASSED, name:', evt.name

  Scenario: At least 1 registration exists for the cricket event
    * def regs = db.query("SELECT * FROM sports_event_registration WHERE event_id = " + verifySportsEventId)
    * assert regs.length >= 1
    * print '✅ Sports registrations count:', regs.length

  Scenario: Sports meta — Cricket Karate Test sport exists as active
    * def sports = db.query("SELECT * FROM sports_meta WHERE name = 'Cricket Karate Test' AND active = true")
    * assert sports.length >= 1
    * print '✅ Cricket Karate Test sport active in DB'

  Scenario: Player categories created for Karate tests
    * def cats = db.query("SELECT * FROM sports_player_category WHERE name LIKE '%Karate Test%'")
    * assert cats.length >= 1
    * print '✅ Karate Test player categories:', cats.length

  Scenario: Sports test users exist in app_user
    * def count = db.scalar("SELECT COUNT(*) FROM app_user WHERE email LIKE '%@sports.test'")
    * assert count >= 2
    * print '✅ Sports test users in DB:', count

  Scenario: Auction config created for the event
    * def auctions = db.query("SELECT * FROM sports_auction_config WHERE season_name LIKE '%Cricket Karate Test Auction%'")
    * assert auctions.length >= 1
    * def auc = auctions[0]
    * print '✅ Auction config in DB — season:', auc.season_name

  Scenario: Auction teams created (at least 2)
    * def auctions = db.query("SELECT id FROM sports_auction_config WHERE season_name LIKE '%Cricket Karate Test Auction%'")
    * if (auctions.length > 0) karate.set('auctionCfgId', auctions[0].id)
    * if (auctions.length > 0) karate.call(read('classpath:karate/features/sports/db/_verify-teams.feature'), { dbAuctionConfigId: auctions[0].id })
    * print '✅ Auction teams DB check done, hasAuction:', auctions.length > 0

  Scenario: No sports data leaked into wrong tables
    # Ensure sports registrations are in sports_event_registration, NOT event_pooja_user_registrations
    * def leaked = db.query("SELECT COUNT(*) as cnt FROM event_pooja_user_registrations r JOIN sports_event e ON r.event_id = e.id WHERE e.name LIKE '%Cricket Karate Test%'")
    * match leaked[0].cnt == 0
    * print '✅ No sports data in pooja registrations table'
