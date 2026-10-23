-- Phase 1: Vendor Commerce Extension
-- Creates the commerce profile table linked one-to-one with vms_vendors,
-- and adds an optional vms_vendor_id FK to vendor_product.

CREATE TABLE IF NOT EXISTS vms_vendor_commerce_profile (
    id                       BIGSERIAL PRIMARY KEY,
    vms_vendor_id            BIGINT NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    commerce_status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    -- Payment
    upi_enabled              BOOLEAN NOT NULL DEFAULT FALSE,
    upi_id                   VARCHAR(80),

    -- Delivery
    delivery_available       BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_radius_km       INTEGER DEFAULT 5,
    avg_delivery_time_minutes INTEGER DEFAULT 60,
    min_order_amount         NUMERIC(19,4) NOT NULL DEFAULT 0,

    -- Pickup
    pickup_available         BOOLEAN NOT NULL DEFAULT TRUE,
    pickup_address           VARCHAR(400),

    -- Storefront
    storefront_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    storefront_description   VARCHAR(1000),
    storefront_banner_url    VARCHAR(500),
    community_visible        BOOLEAN NOT NULL DEFAULT FALSE,

    -- Verification
    verified_at              TIMESTAMP,
    verified_by_user_id      BIGINT,
    rejection_reason         VARCHAR(500),
    submitted_at             TIMESTAMP,

    -- Audit
    created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_by               BIGINT,

    CONSTRAINT uq_vendor_commerce_profile UNIQUE (vms_vendor_id),
    CONSTRAINT chk_commerce_status CHECK (commerce_status IN (
        'DRAFT','PENDING_VERIFICATION','VERIFIED','SUSPENDED','REJECTED','INACTIVE'
    ))
);

CREATE INDEX IF NOT EXISTS idx_vcp_vendor_id     ON vms_vendor_commerce_profile (vms_vendor_id);
CREATE INDEX IF NOT EXISTS idx_vcp_status        ON vms_vendor_commerce_profile (commerce_status);
CREATE INDEX IF NOT EXISTS idx_vcp_community_visible ON vms_vendor_commerce_profile (community_visible)
    WHERE community_visible = TRUE;

-- Add optional VmsVendor link to vendor_product (nullable — existing rows have no vendor)
ALTER TABLE vendor_product
    ADD COLUMN IF NOT EXISTS vms_vendor_id BIGINT REFERENCES vms_vendors(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_vp_vms_vendor_id ON vendor_product (vms_vendor_id)
    WHERE vms_vendor_id IS NOT NULL;
