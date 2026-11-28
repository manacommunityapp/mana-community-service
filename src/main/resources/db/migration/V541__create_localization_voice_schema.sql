-- V541: OTA Vernacular Localization Bundles, TTS Audio Cache & Voice-First Gate Entry

CREATE SCHEMA IF NOT EXISTS localization;

CREATE TABLE IF NOT EXISTS localization.ota_translation_bundles (
    bundle_id              UUID PRIMARY KEY,
    locale                 VARCHAR(15) NOT NULL,
    namespace              VARCHAR(60) NOT NULL,
    version_hash           CHAR(64) NOT NULL,
    direction              VARCHAR(3) NOT NULL DEFAULT 'ltr',
    font_family            VARCHAR(80) NOT NULL DEFAULT 'NotoSans',
    line_height_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.5,
    strings_json           JSONB NOT NULL DEFAULT '{}'::jsonb,
    tts_hints_json         JSONB NOT NULL DEFAULT '{}'::jsonb,
    audio_pack_url         VARCHAR(1000),
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    published_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ota_locale_ns_hash UNIQUE (locale, namespace, version_hash)
);

CREATE TABLE IF NOT EXISTS localization.tts_audio_cache (
    cache_key       CHAR(64) PRIMARY KEY,
    locale          VARCHAR(15) NOT NULL,
    voice_id        VARCHAR(80) NOT NULL,
    normalized_text TEXT NOT NULL,
    audio_url       VARCHAR(1000) NOT NULL,
    codec           VARCHAR(20) NOT NULL DEFAULT 'OPUS_16K',
    duration_ms     INTEGER NOT NULL DEFAULT 1500,
    size_bytes      INTEGER NOT NULL DEFAULT 5120,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS localization.voice_gate_entries (
    entry_id              UUID PRIMARY KEY,
    society_id            BIGINT NOT NULL,
    guard_user_id         VARCHAR(100) NOT NULL,
    flat_number           VARCHAR(40) NOT NULL,
    guard_locale          VARCHAR(15) NOT NULL DEFAULT 'te',
    resident_locale       VARCHAR(15) NOT NULL DEFAULT 'en',
    audio_url             VARCHAR(1000) NOT NULL,
    audio_codec           VARCHAR(30) NOT NULL DEFAULT 'OPUS_16K',
    duration_ms           INTEGER NOT NULL DEFAULT 2000,
    audio_size_bytes      INTEGER NOT NULL DEFAULT 5120,
    transcript_raw        TEXT,
    transcript_translated TEXT,
    detected_visitor_name VARCHAR(150),
    detected_org_name     VARCHAR(150),
    entity_confidence     DOUBLE PRECISION,
    status                VARCHAR(30) NOT NULL DEFAULT 'DISPATCHED_AUDIO',
    expires_at            TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transcribed_at        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ota_bundles_active ON localization.ota_translation_bundles (locale, namespace, active);
CREATE INDEX IF NOT EXISTS idx_voice_gate_society_created ON localization.voice_gate_entries (society_id, created_at DESC);
