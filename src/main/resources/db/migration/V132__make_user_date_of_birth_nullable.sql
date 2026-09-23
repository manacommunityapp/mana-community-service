-- Make date_of_birth column nullable in app_user
ALTER TABLE manacommunity.app_user ALTER COLUMN date_of_birth DROP NOT NULL;
