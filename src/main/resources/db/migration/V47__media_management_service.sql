-- ── V47: Centralized Media Management Service ────────────────────────────
-- Creates three tables:
--   media_objects       – permanent metadata for every uploaded file
--   media_upload_sessions – tracks presigned-PUT sessions (video/large file direct upload)
--   media_audit_log     – immutable event trail (upload, approve, delete, restore)
--
-- Rules enforced here:
--   • Files are NEVER stored in PostgreSQL. Only metadata + S3 keys.
--   • URLs are NEVER stored. Generated dynamically from s3_key at query time.
--   • Soft delete via `deleted` flag — physical S3 deletion is async.

CREATE TABLE IF NOT EXISTS manacommunity.media_objects
(
    id                  BIGSERIAL PRIMARY KEY,
    external_id         UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- Module context
    module              VARCHAR(50)  NOT NULL,
    module_id           VARCHAR(100) NOT NULL,
    community_id        BIGINT       NOT NULL,
    sub_context         VARCHAR(100),

    -- File identity
    original_file_name  VARCHAR(512) NOT NULL,
    stored_file_name    VARCHAR(512) NOT NULL,
    mime_type           VARCHAR(128) NOT NULL,
    extension           VARCHAR(20)  NOT NULL,
    file_size           BIGINT       NOT NULL,

    -- Media classification
    media_type          VARCHAR(20)  NOT NULL
        CHECK (media_type IN ('IMAGE', 'VIDEO', 'DOCUMENT', 'AUDIO', 'CERTIFICATE', 'QR_CODE')),

    -- S3 storage keys (NEVER store full URLs)
    bucket_name         VARCHAR(255)  NOT NULL,
    s3_key              VARCHAR(1024) NOT NULL,
    thumbnail_key       VARCHAR(1024),
    compressed_key      VARCHAR(1024),
    medium_key          VARCHAR(1024),

    -- Dimensional metadata
    width               INT,
    height              INT,
    duration_seconds    INT,

    -- Processing pipeline status
    status              VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('PENDING', 'PROCESSING', 'ACTIVE', 'REJECTED', 'DELETED', 'ARCHIVED')),
    processing_status   VARCHAR(30)           DEFAULT 'COMPLETE'
        CHECK (processing_status IN ('QUEUED', 'THUMBNAIL_GENERATED', 'COMPRESSED', 'COMPLETE', 'FAILED')),

    -- Approval workflow
    approval_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    approved_by         BIGINT,
    approved_at         TIMESTAMPTZ,
    rejection_reason    TEXT,

    -- Ownership
    uploaded_by         BIGINT       NOT NULL,
    uploaded_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Soft delete
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_by          BIGINT,
    deleted_at          TIMESTAMPTZ,

    -- Enrichment
    caption             TEXT,
    alt_text            VARCHAR(512),
    featured            BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order          INT                   DEFAULT 0,

    -- Audit timestamps
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_media_external_id UNIQUE (external_id),
    CONSTRAINT uq_media_s3_key      UNIQUE (s3_key)
);

CREATE INDEX IF NOT EXISTS idx_media_module      ON manacommunity.media_objects (module, module_id);
CREATE INDEX IF NOT EXISTS idx_media_community   ON manacommunity.media_objects (community_id);
CREATE INDEX IF NOT EXISTS idx_media_status      ON manacommunity.media_objects (status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_media_uploaded_by ON manacommunity.media_objects (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_media_featured    ON manacommunity.media_objects (featured) WHERE status = 'ACTIVE' AND deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_media_ext_id      ON manacommunity.media_objects (external_id);
CREATE INDEX IF NOT EXISTS idx_media_uploaded_at ON manacommunity.media_objects (uploaded_at DESC);

-- Upload sessions (presigned-PUT flow for video / large files)
CREATE TABLE IF NOT EXISTS manacommunity.media_upload_sessions
(
    id            BIGSERIAL PRIMARY KEY,
    session_id    UUID          NOT NULL DEFAULT gen_random_uuid(),
    module        VARCHAR(50)   NOT NULL,
    module_id     VARCHAR(100)  NOT NULL,
    community_id  BIGINT        NOT NULL,
    sub_context   VARCHAR(100),
    uploaded_by   BIGINT        NOT NULL,
    presigned_key VARCHAR(1024) NOT NULL,
    bucket        VARCHAR(255)  NOT NULL,
    media_type    VARCHAR(20)   NOT NULL,
    original_name VARCHAR(512),
    file_size     BIGINT,
    mime_type     VARCHAR(128),
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'FAILED')),
    expires_at    TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_upload_session_id UNIQUE (session_id)
);

CREATE INDEX IF NOT EXISTS idx_upload_session_status ON manacommunity.media_upload_sessions (status, expires_at);

-- Audit log (immutable trail)
CREATE TABLE IF NOT EXISTS manacommunity.media_audit_log
(
    id         BIGSERIAL PRIMARY KEY,
    media_id   BIGINT       NOT NULL,
    action     VARCHAR(50)  NOT NULL,
    actor_id   BIGINT       NOT NULL,
    actor_role VARCHAR(50),
    ip_address VARCHAR(45),
    detail     TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_media_audit_media ON manacommunity.media_audit_log (media_id);
CREATE INDEX IF NOT EXISTS idx_media_audit_actor ON manacommunity.media_audit_log (actor_id);
