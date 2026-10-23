-- V157: Rename venue, venue_contact and event_venue_config tables to follow sports_ prefix convention

-- ── 1. venue → sports_venue ───────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'manacommunity' AND table_name = 'venue')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables
                       WHERE table_schema = 'manacommunity' AND table_name = 'sports_venue')
    THEN
        ALTER TABLE manacommunity.venue RENAME TO sports_venue;
        RAISE NOTICE 'Renamed venue to sports_venue';
    ELSIF EXISTS (SELECT 1 FROM information_schema.tables
                  WHERE table_schema = 'manacommunity' AND table_name = 'venue')
       AND EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'manacommunity' AND table_name = 'sports_venue')
    THEN
        DROP TABLE IF EXISTS manacommunity.venue CASCADE;
        RAISE NOTICE 'Dropped old venue table (sports_venue already exists)';
    END IF;
END $$;

-- ── 2. venue_contact → sports_venue_contact ───────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'manacommunity' AND table_name = 'venue_contact')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables
                       WHERE table_schema = 'manacommunity' AND table_name = 'sports_venue_contact')
    THEN
        ALTER TABLE manacommunity.venue_contact RENAME TO sports_venue_contact;
        RAISE NOTICE 'Renamed venue_contact to sports_venue_contact';
    ELSIF EXISTS (SELECT 1 FROM information_schema.tables
                  WHERE table_schema = 'manacommunity' AND table_name = 'venue_contact')
       AND EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'manacommunity' AND table_name = 'sports_venue_contact')
    THEN
        DROP TABLE IF EXISTS manacommunity.venue_contact CASCADE;
        RAISE NOTICE 'Dropped old venue_contact table (sports_venue_contact already exists)';
    END IF;
END $$;

-- ── 3. event_venue_config → sports_event_venue_config ─────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'manacommunity' AND table_name = 'event_venue_config')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables
                       WHERE table_schema = 'manacommunity' AND table_name = 'sports_event_venue_config')
    THEN
        ALTER TABLE manacommunity.event_venue_config RENAME TO sports_event_venue_config;
        RAISE NOTICE 'Renamed event_venue_config to sports_event_venue_config';
    ELSIF EXISTS (SELECT 1 FROM information_schema.tables
                  WHERE table_schema = 'manacommunity' AND table_name = 'event_venue_config')
       AND EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'manacommunity' AND table_name = 'sports_event_venue_config')
    THEN
        DROP TABLE IF EXISTS manacommunity.event_venue_config CASCADE;
        RAISE NOTICE 'Dropped old event_venue_config table (sports_event_venue_config already exists)';
    END IF;
END $$;
