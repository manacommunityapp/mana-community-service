ALTER TABLE vms_vendor_registrations
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS description VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pincode VARCHAR(10),
    ADD COLUMN IF NOT EXISTS gst_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS applicant_user_id BIGINT REFERENCES app_user(id),
    ADD COLUMN IF NOT EXISTS reviewed_by BIGINT REFERENCES app_user(id),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_vms_vendor_registrations_applicant
    ON vms_vendor_registrations(community_id, applicant_user_id, status);
