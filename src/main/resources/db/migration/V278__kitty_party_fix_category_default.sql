-- Fix default category_code from 'KITTY' to 'KITTY_PARTY' to match mobile app enum values
ALTER TABLE manacommunity.kitty_circle ALTER COLUMN category_code SET DEFAULT 'KITTY_PARTY';

-- Migrate any existing rows with the old default
UPDATE manacommunity.kitty_circle SET category_code = 'KITTY_PARTY' WHERE category_code = 'KITTY';
