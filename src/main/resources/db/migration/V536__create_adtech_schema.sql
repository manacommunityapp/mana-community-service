-- V100: Hyperlocal AdTech Campaigns, Impressions, Sample Vouchers & Offline Kiosk Redemptions
-- Mirrors mana-community-liquibase domains/adtech schema for standalone Flyway deployments.

CREATE SCHEMA IF NOT EXISTS adtech;

CREATE TABLE IF NOT EXISTS adtech.ad_campaigns (
    id                  BIGSERIAL PRIMARY KEY,
    campaign_id         UUID NOT NULL UNIQUE,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    brand_id            VARCHAR(100) NOT NULL,
    brand_name          VARCHAR(255) NOT NULL,
    name                VARCHAR(300) NOT NULL,
    total_budget_paise  BIGINT NOT NULL,
    spent_paise         BIGINT NOT NULL DEFAULT 0,
    cpm_paise           BIGINT NOT NULL,
    daily_cap_paise     BIGINT,
    freq_cap_per_day    INTEGER NOT NULL DEFAULT 3,
    targeting           JSONB NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    start_at            TIMESTAMPTZ NOT NULL,
    end_at              TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT NOT NULL DEFAULT 0,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS adtech.ad_creatives (
    id              BIGSERIAL PRIMARY KEY,
    creative_id     UUID NOT NULL UNIQUE,
    campaign_id     UUID NOT NULL REFERENCES adtech.ad_campaigns(campaign_id),
    creative_type   VARCHAR(30) NOT NULL,
    asset_url       VARCHAR(1000) NOT NULL,
    cta_url         VARCHAR(1000) NOT NULL,
    cta_text        VARCHAR(100),
    headline        VARCHAR(300),
    body_text       TEXT,
    brand_logo_url  VARCHAR(1000),
    weight          INTEGER NOT NULL DEFAULT 100,
    width_px        INTEGER,
    height_px       INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS adtech.ad_impressions (
    impression_id   UUID PRIMARY KEY,
    campaign_id     UUID NOT NULL,
    user_id         VARCHAR(100) NOT NULL,
    society_id      VARCHAR(100) NOT NULL,
    placement_id    VARCHAR(100) NOT NULL,
    cost_paise      BIGINT NOT NULL DEFAULT 0,
    fired_at        TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS adtech.ad_clicks (
    id              BIGSERIAL PRIMARY KEY,
    impression_id   UUID NOT NULL,
    campaign_id     UUID NOT NULL,
    clicked_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS adtech.sample_vouchers (
    id                  VARCHAR(100) PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    campaign_id         VARCHAR(100) NOT NULL,
    campaign_name       VARCHAR(255) NOT NULL,
    brand_name          VARCHAR(255) NOT NULL,
    brand_logo_url      VARCHAR(1000),
    product_name        VARCHAR(255) NOT NULL,
    product_image_url   VARCHAR(1000),
    description         TEXT,
    society_id          VARCHAR(100) NOT NULL,
    kiosk_location      VARCHAR(255),
    kiosk_open_from     VARCHAR(50),
    kiosk_open_until    VARCHAR(50),
    valid_from          TIMESTAMPTZ NOT NULL,
    valid_until         TIMESTAMPTZ NOT NULL,
    max_claims          INTEGER,
    hmac_key_encrypted  TEXT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS adtech.voucher_claims (
    vid                     VARCHAR(100) PRIMARY KEY,
    voucher_id              VARCHAR(100) NOT NULL REFERENCES adtech.sample_vouchers(id),
    cid                     VARCHAR(100) NOT NULL,
    uid                     VARCHAR(100) NOT NULL,
    sid                     VARCHAR(100) NOT NULL,
    nonce                   VARCHAR(32) NOT NULL,
    hmac_suffix             VARCHAR(128) NOT NULL,
    totp_secret_encrypted   TEXT NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CLAIMED',
    issued_at               TIMESTAMPTZ NOT NULL,
    expires_at              TIMESTAMPTZ NOT NULL,
    redeemed_at             TIMESTAMPTZ,
    CONSTRAINT uq_voucher_claims_campaign_user UNIQUE (cid, uid)
);

CREATE TABLE IF NOT EXISTS adtech.voucher_redemptions (
    vid             VARCHAR(100) PRIMARY KEY,
    uid             VARCHAR(100) NOT NULL,
    kiosk_id        VARCHAR(100) NOT NULL,
    redeemed_at     TIMESTAMPTZ NOT NULL,
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_conflicted   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS adtech.kiosk_devices (
    kiosk_id        VARCHAR(100) PRIMARY KEY,
    society_id      VARCHAR(100) NOT NULL,
    label           VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_sync_at    TIMESTAMPTZ,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ad_campaigns_status_dates ON adtech.ad_campaigns (status, start_at, end_at);
CREATE INDEX IF NOT EXISTS idx_ad_impressions_campaign_fired ON adtech.ad_impressions (campaign_id, fired_at);
CREATE INDEX IF NOT EXISTS idx_ad_impressions_user_campaign ON adtech.ad_impressions (user_id, campaign_id);
CREATE INDEX IF NOT EXISTS idx_sample_vouchers_society ON adtech.sample_vouchers (society_id, valid_until);
CREATE INDEX IF NOT EXISTS idx_voucher_claims_uid ON adtech.voucher_claims (uid, status);
