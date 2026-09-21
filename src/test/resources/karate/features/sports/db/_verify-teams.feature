@ignore
Feature: Verify auction teams exist in DB for a config
  # Requires: dbAuctionConfigId

  Background:
    * def DbUtils = Java.type('karate.DbUtils')
    * def db      = new DbUtils(dbUrl, dbUser, dbPassword)

  Scenario: Check team count
    * def teams = db.query("SELECT * FROM sports_auction_team WHERE config_id = " + dbAuctionConfigId)
    * assert teams.length >= 2
    * print '  [verify-teams] teams in DB for config', dbAuctionConfigId, ':', teams.length
