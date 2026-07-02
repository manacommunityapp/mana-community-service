-- Widen govt_id_number to hold AES-256-GCM ciphertext (Base64-encoded).
-- Existing plaintext values will need a one-time migration to encrypted form.
ALTER TABLE app_user ALTER COLUMN govt_id_number TYPE VARCHAR(255);
