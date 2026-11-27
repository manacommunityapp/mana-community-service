-- Health sensitive columns are now AES-256-GCM encrypted at the application layer.
-- Existing plaintext rows will be re-encrypted on next write.
-- Column type kept as TEXT/VARCHAR to hold Base64-encoded ciphertext.
-- Encrypted fields in health_profile: allergies, medical_conditions, emergency_notes,
--   medical_id_number, physician_name, physician_phone
-- Encrypted fields in emergency_contact: name, phone, email

COMMENT ON COLUMN manacommunity.health_profile.allergies IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.health_profile.medical_conditions IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.health_profile.emergency_notes IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.health_profile.medical_id_number IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.health_profile.physician_name IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.health_profile.physician_phone IS 'AES-256-GCM encrypted at application layer';

COMMENT ON COLUMN manacommunity.emergency_contact.name IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.emergency_contact.phone IS 'AES-256-GCM encrypted at application layer';
COMMENT ON COLUMN manacommunity.emergency_contact.email IS 'AES-256-GCM encrypted at application layer';
