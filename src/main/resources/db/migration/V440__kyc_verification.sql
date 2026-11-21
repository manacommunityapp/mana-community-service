CREATE TABLE kyc_verification_requests (
  id                  BIGSERIAL PRIMARY KEY,
  community_id        BIGINT NOT NULL,
  resident_id         BIGINT NOT NULL,
  household_member_id BIGINT,
  verification_method VARCHAR(50) NOT NULL DEFAULT 'MANUAL_REVIEW',
  id_type             VARCHAR(50),
  id_number_hash      VARCHAR(100),
  document_url        VARCHAR(500),
  status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  verified_by         BIGINT,
  rejection_reason    TEXT,
  verified_at         TIMESTAMPTZ,
  expires_at          TIMESTAMPTZ,
  external_ref        VARCHAR(200),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kyc_resident   ON kyc_verification_requests(resident_id, status);
CREATE INDEX idx_kyc_community  ON kyc_verification_requests(community_id, status);
