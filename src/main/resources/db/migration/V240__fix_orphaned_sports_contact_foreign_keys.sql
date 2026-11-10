-- V240: Clean up orphaned contact_id references across sports join tables
-- (sports_venue_contact, sports_event_contact, sports_tournament_contact)
-- so Hibernate / PostgreSQL foreign key constraints can be applied without violation.

DO $$
BEGIN
    -- 1. sports_venue_contact
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_venue_contact'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'manacommunity' 
              AND table_name = 'contact'
        ) THEN
            DELETE FROM manacommunity.sports_venue_contact
            WHERE contact_id NOT IN (SELECT id FROM manacommunity.contact);
        ELSE
            DELETE FROM manacommunity.sports_venue_contact;
        END IF;
    END IF;

    -- 2. sports_event_contact
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_event_contact'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'manacommunity' 
              AND table_name = 'contact'
        ) THEN
            DELETE FROM manacommunity.sports_event_contact
            WHERE contact_id NOT IN (SELECT id FROM manacommunity.contact);
        ELSE
            DELETE FROM manacommunity.sports_event_contact;
        END IF;
    END IF;

    -- 3. sports_tournament_contact
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'manacommunity' 
          AND table_name = 'sports_tournament_contact'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'manacommunity' 
              AND table_name = 'contact'
        ) THEN
            DELETE FROM manacommunity.sports_tournament_contact
            WHERE contact_id NOT IN (SELECT id FROM manacommunity.contact);
        ELSE
            DELETE FROM manacommunity.sports_tournament_contact;
        END IF;
    END IF;
END $$;
