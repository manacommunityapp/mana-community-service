-- Rename player_category → sports_player_category if V101 didn't apply
-- If both tables exist (Hibernate created sports_player_category separately),
-- migrate data from old table into new and drop the old one.
-- Uses explicit column names + casts to handle schema differences.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'manacommunity' AND table_name = 'player_category')
    THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                       WHERE table_schema = 'manacommunity' AND table_name = 'sports_player_category')
        THEN
            ALTER TABLE manacommunity.player_category RENAME TO sports_player_category;
        ELSE
            -- Both tables exist: copy rows from old table not already in new, casting types as needed
            INSERT INTO manacommunity.sports_player_category (id, name, category_type, description, min_age, max_age, gender, type, community_id)
                SELECT pc.id, pc.name, pc.category_type, pc.description,
                       CAST(pc.min_age AS INTEGER), CAST(pc.max_age AS INTEGER),
                       pc.gender, pc.type, pc.community_id
                FROM manacommunity.player_category pc
                WHERE NOT EXISTS (SELECT 1 FROM manacommunity.sports_player_category spc WHERE spc.id = pc.id)
            ON CONFLICT DO NOTHING;
            DROP TABLE manacommunity.player_category CASCADE;
        END IF;
    END IF;
END $$;

-- Recreate FK pointing to the correct table
ALTER TABLE manacommunity.sports_event_registration
    DROP CONSTRAINT IF EXISTS fk_sports_event_registration_category_id;

ALTER TABLE manacommunity.sports_event_registration
    ADD CONSTRAINT fk_sports_event_registration_category_id
    FOREIGN KEY (category_id) REFERENCES manacommunity.sports_player_category(id);
