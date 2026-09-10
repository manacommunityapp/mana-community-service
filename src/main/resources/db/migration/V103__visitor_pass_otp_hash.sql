-- Migrate visitor_pass.otp (plain text, 6 chars) to otp_hash (SHA-256, 64 chars).
-- Plain OTPs are expired by design (12h max TTL), so historical rows are safe to nullify.
-- Going forward, only SHA-256 hashes are stored; the plain OTP is returned once at creation.

-- 1. Add the new hashed OTP column and attempt counter
ALTER TABLE manacommunity.visitor_pass
    ADD COLUMN IF NOT EXISTS otp_hash     VARCHAR(64),
    ADD COLUMN IF NOT EXISTS otp_attempts INT NOT NULL DEFAULT 0;

-- 2. Nullify all existing plain-text OTPs (they are all expired)
UPDATE manacommunity.visitor_pass SET otp_hash = NULL WHERE otp IS NOT NULL;

-- 3. Drop the insecure plain-text OTP column
ALTER TABLE manacommunity.visitor_pass DROP COLUMN IF EXISTS otp;
