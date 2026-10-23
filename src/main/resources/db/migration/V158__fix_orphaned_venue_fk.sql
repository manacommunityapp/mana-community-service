-- V158: Clean up orphaned venue_id references across sports tables
-- Nullify or remove orphaned references pointing to non-existent venue IDs
-- so Hibernate / PostgreSQL foreign key constraints can be applied without violation.

DO $$
BEGIN
    -- 1. sports_event
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_event' 
          AND column_name = 'venue_id'
    ) THEN
        UPDATE manacommunity.sports_event
        SET venue_id = NULL
        WHERE venue_id IS NOT NULL
          AND venue_id NOT IN (SELECT id FROM manacommunity.sports_venue);
    END IF;

    -- 2. sports_tournament_match
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_tournament_match' 
          AND column_name = 'venue_id'
    ) THEN
        UPDATE manacommunity.sports_tournament_match
        SET venue_id = NULL
        WHERE venue_id IS NOT NULL
          AND venue_id NOT IN (SELECT id FROM manacommunity.sports_venue);
    END IF;

    -- 3. sports_tournament_config
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_tournament_config' 
          AND column_name = 'venue_id'
    ) THEN
        UPDATE manacommunity.sports_tournament_config
        SET venue_id = NULL
        WHERE venue_id IS NOT NULL
          AND venue_id NOT IN (SELECT id FROM manacommunity.sports_venue);
    END IF;

    -- 4. sports_venue_contact
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_venue_contact'
    ) THEN
        DELETE FROM manacommunity.sports_venue_contact
        WHERE venue_id NOT IN (SELECT id FROM manacommunity.sports_venue);
    END IF;

    -- 5. sports_court
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_court' 
          AND column_name = 'venue_id'
    ) THEN
        DELETE FROM manacommunity.sports_court
        WHERE venue_id NOT IN (SELECT id FROM manacommunity.sports_venue);
    END IF;
END $$;
