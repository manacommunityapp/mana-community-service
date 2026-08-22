-- V67: Add sender and HTML body to email delivery logs for admin inspection and resending
ALTER TABLE email_delivery_log ADD COLUMN IF NOT EXISTS sender VARCHAR(255);
ALTER TABLE email_delivery_log ADD COLUMN IF NOT EXISTS body TEXT;
