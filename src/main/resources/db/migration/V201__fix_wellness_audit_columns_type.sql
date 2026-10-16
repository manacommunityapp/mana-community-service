-- Fix created_by and updated_by column types from VARCHAR(255) to BIGINT
-- to match Java BaseAuditEntity (Long createdBy, Long updatedBy)

DO $$
BEGIN
    -- wellness_profile
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'wellness_profile' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.wellness_profile 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- wellness_challenge
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'wellness_challenge' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.wellness_challenge 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- wellness_provider
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'wellness_provider' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.wellness_provider 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- meal_log
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'meal_log' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.meal_log 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- meal_item
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'meal_item' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.meal_item 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- water_log
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'water_log' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.water_log 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- food_catalog
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'food_catalog' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.food_catalog 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- challenge_participant
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'challenge_participant' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.challenge_participant 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;

    -- accountability_partner
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'accountability_partner' AND column_name = 'updated_by' AND data_type = 'character varying'
    ) THEN
        ALTER TABLE manacommunity.accountability_partner 
            ALTER COLUMN created_by TYPE BIGINT USING (CASE WHEN created_by ~ '^[0-9]+$' THEN created_by::BIGINT ELSE NULL END),
            ALTER COLUMN updated_by TYPE BIGINT USING (CASE WHEN updated_by ~ '^[0-9]+$' THEN updated_by::BIGINT ELSE NULL END);
    END IF;
END $$;
