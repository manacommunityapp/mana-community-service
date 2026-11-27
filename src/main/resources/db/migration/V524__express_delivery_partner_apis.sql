-- ==============================================================================
-- Flyway Migration: V524__express_delivery_partner_apis.sql
-- Description: Tables for Specialized Domain APIs: Express Entry (Zomato, Swiggy, Blinkit, etc.)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS express_delivery_passes (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    partner VARCHAR(32) NOT NULL,
    community_id BIGINT NOT NULL,
    flat_number VARCHAR(64) NOT NULL,
    resident_name VARCHAR(128),
    resident_phone VARCHAR(32),
    rider_name VARCHAR(128) NOT NULL,
    rider_phone VARCHAR(32),
    vehicle_number VARCHAR(32),
    rider_photo_url VARCHAR(512),
    entry_pass_code VARCHAR(32) NOT NULL UNIQUE,
    barcode_payload VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PRE_APPROVED',
    leave_at_gate BOOLEAN NOT NULL DEFAULT FALSE,
    gate_instructions VARCHAR(512),
    items_summary VARCHAR(256),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    entry_gate VARCHAR(64),
    exit_gate VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_express_passes_code ON express_delivery_passes (entry_pass_code);
CREATE INDEX IF NOT EXISTS idx_express_passes_barcode ON express_delivery_passes (barcode_payload);
CREATE INDEX IF NOT EXISTS idx_express_passes_partner_order ON express_delivery_passes (partner, order_id);
CREATE INDEX IF NOT EXISTS idx_express_passes_community_status ON express_delivery_passes (community_id, status);

CREATE TABLE IF NOT EXISTS resident_delivery_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    community_id BIGINT NOT NULL,
    flat_number VARCHAR(64) NOT NULL,
    auto_approve_swiggy BOOLEAN NOT NULL DEFAULT TRUE,
    auto_approve_zomato BOOLEAN NOT NULL DEFAULT TRUE,
    auto_approve_blinkit BOOLEAN NOT NULL DEFAULT TRUE,
    auto_approve_zepto BOOLEAN NOT NULL DEFAULT TRUE,
    auto_approve_amazon BOOLEAN NOT NULL DEFAULT TRUE,
    auto_approve_other BOOLEAN NOT NULL DEFAULT TRUE,
    default_leave_at_gate BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_instructions VARCHAR(512),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_resident_pref_community_flat UNIQUE (community_id, flat_number)
);

CREATE INDEX IF NOT EXISTS idx_resident_pref_community_flat ON resident_delivery_preferences (community_id, flat_number);
