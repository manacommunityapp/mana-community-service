-- ── V101: Rename all Sports-domain tables to carry the sports_ prefix ────────
-- Safe in Postgres: ALTER TABLE … RENAME preserves all FKs, indexes, and
-- sequences. No data is touched. Run with ddl-auto=validate after deploying
-- the corresponding Java @Table/@JoinTable annotation changes.

-- ── Tournament cluster ────────────────────────────────────────────────────────
ALTER TABLE tournament                RENAME TO sports_tournament;
ALTER TABLE tournament_announcement   RENAME TO sports_tournament_announcement;
ALTER TABLE tournament_gallery_image  RENAME TO sports_tournament_gallery_image;
ALTER TABLE tournament_timeline_entry RENAME TO sports_tournament_timeline_entry;
ALTER TABLE tournament_contact        RENAME TO sports_tournament_contact;
ALTER TABLE tournament_config         RENAME TO sports_tournament_config;
ALTER TABLE tournament_group          RENAME TO sports_tournament_group;
ALTER TABLE tournament_match          RENAME TO sports_tournament_match;

-- ── Auction cluster ───────────────────────────────────────────────────────────
ALTER TABLE auction_config            RENAME TO sports_auction_config;
ALTER TABLE auction_config_category   RENAME TO sports_auction_config_category;
ALTER TABLE auction_team              RENAME TO sports_auction_team;
ALTER TABLE auction_player            RENAME TO sports_auction_player;
ALTER TABLE auction_bid               RENAME TO sports_auction_bid;
ALTER TABLE auction_dispute_committee RENAME TO sports_auction_dispute_committee;
ALTER TABLE auction_session_log       RENAME TO sports_auction_session_log;

-- ── Scheduler / match scorecards ──────────────────────────────────────────────
ALTER TABLE group_team_standing       RENAME TO sports_group_team_standing;
ALTER TABLE schedule_generation_log   RENAME TO sports_schedule_generation_log;
ALTER TABLE match_result              RENAME TO sports_match_result;
ALTER TABLE match_innings             RENAME TO sports_match_innings;
ALTER TABLE batting_performance       RENAME TO sports_batting_performance;
ALTER TABLE bowling_performance       RENAME TO sports_bowling_performance;
ALTER TABLE match_ball_event          RENAME TO sports_match_ball_event;
ALTER TABLE player_tournament_stats   RENAME TO sports_player_tournament_stats;

-- ── Core sports tables ────────────────────────────────────────────────────────
ALTER TABLE player_category           RENAME TO sports_player_category;
ALTER TABLE court                     RENAME TO sports_court;
ALTER TABLE event_category            RENAME TO sports_event_category;
