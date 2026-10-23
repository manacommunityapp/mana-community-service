-- FILE: db/migration/V123__add_category_options_to_sports_event.sql
-- Adds allow_higher_age_category and allow_multiple_categories columns to sports_event.

ALTER TABLE sports_event
    ADD COLUMN IF NOT EXISTS allow_higher_age_category BOOLEAN DEFAULT TRUE NOT NULL,
    ADD COLUMN IF NOT EXISTS allow_multiple_categories BOOLEAN DEFAULT TRUE NOT NULL;
