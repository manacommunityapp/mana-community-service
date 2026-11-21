CREATE TYPE document_category AS ENUM (
  'RENTAL_AGREEMENT', 'DOCTOR_DOCUMENT', 'VENDOR_KYC',
  'SPORTS_WAIVER', 'INSURANCE_DOCUMENT', 'INVOICE',
  'EVENT_SPONSORSHIP_CONTRACT', 'GOVERNANCE_RECORD', 'OTHER'
);

CREATE TYPE document_status AS ENUM (
  'PENDING_SCAN', 'ACTIVE', 'EXPIRED', 'ARCHIVED', 'DELETED'
);

CREATE TYPE doc_scan_status AS ENUM (
  'PENDING', 'CLEAN', 'INFECTED', 'SCAN_FAILED'
);

CREATE TYPE doc_access_level AS ENUM (
  'OWNER_ONLY', 'COMMUNITY_ADMIN', 'COMMUNITY', 'PUBLIC'
);

CREATE TABLE document (
  id                   BIGSERIAL PRIMARY KEY,
  community_id         BIGINT       NOT NULL,
  title                VARCHAR(255) NOT NULL,
  description          TEXT,
  category             document_category NOT NULL,
  entity_type          VARCHAR(60),
  entity_id            VARCHAR(100),
  owner_user_id        BIGINT,
  status               document_status NOT NULL DEFAULT 'PENDING_SCAN',
  access_level         doc_access_level NOT NULL DEFAULT 'OWNER_ONLY',
  expires_at           TIMESTAMP,
  reminder_days_before INT          NOT NULL DEFAULT 30,
  retention_days       INT,
  legal_hold           BOOLEAN      NOT NULL DEFAULT FALSE,
  created_by           BIGINT,
  created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE document_version (
  id                BIGSERIAL PRIMARY KEY,
  document_id       BIGINT       NOT NULL REFERENCES document(id),
  version_number    INT          NOT NULL DEFAULT 1,
  s3_key            VARCHAR(500) NOT NULL,
  s3_bucket         VARCHAR(200) NOT NULL,
  original_filename VARCHAR(500) NOT NULL,
  content_type      VARCHAR(100),
  file_size_bytes   BIGINT,
  checksum_sha256   VARCHAR(64),
  scan_status       doc_scan_status NOT NULL DEFAULT 'PENDING',
  scan_completed_at TIMESTAMP,
  is_current        BOOLEAN      NOT NULL DEFAULT TRUE,
  uploaded_by       BIGINT,
  uploaded_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
  UNIQUE(document_id, version_number)
);

CREATE TABLE document_access_grant (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT      NOT NULL REFERENCES document(id),
  grantee_type VARCHAR(30) NOT NULL,
  grantee_id   VARCHAR(100) NOT NULL,
  permission   VARCHAR(30) NOT NULL,
  granted_by   BIGINT,
  granted_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
  expires_at   TIMESTAMP,
  UNIQUE(document_id, grantee_type, grantee_id, permission)
);

CREATE TABLE document_download_log (
  id                  BIGSERIAL PRIMARY KEY,
  document_id         BIGINT    NOT NULL REFERENCES document(id),
  version_id          BIGINT    NOT NULL REFERENCES document_version(id),
  downloader_user_id  BIGINT,
  ip_address          VARCHAR(45),
  downloaded_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_community   ON document(community_id, status);
CREATE INDEX idx_doc_entity      ON document(entity_type, entity_id);
CREATE INDEX idx_doc_owner       ON document(owner_user_id);
CREATE INDEX idx_doc_category    ON document(community_id, category);
CREATE INDEX idx_doc_expires     ON document(expires_at) WHERE expires_at IS NOT NULL AND status = 'ACTIVE';
CREATE INDEX idx_dv_document     ON document_version(document_id, is_current);
CREATE INDEX idx_ddl_document    ON document_download_log(document_id, downloaded_at DESC);
CREATE INDEX idx_dag_document    ON document_access_grant(document_id);
