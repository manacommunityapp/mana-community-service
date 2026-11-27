-- Hyperlocal Delivery Batch Manifests & Stateful Overstay Reconciliation

CREATE TABLE IF NOT EXISTS express_delivery_manifests (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    partner VARCHAR(32) NOT NULL,
    manifest_code VARCHAR(32) NOT NULL UNIQUE,
    rider_name VARCHAR(120) NOT NULL,
    rider_phone VARCHAR(32) NOT NULL,
    vehicle_number VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'PRE_APPROVED',
    total_packages INT NOT NULL DEFAULT 0,
    distinct_flats INT NOT NULL DEFAULT 0,
    delivered_packages INT NOT NULL DEFAULT 0,
    pending_packages INT NOT NULL DEFAULT 0,
    entry_gate VARCHAR(64),
    exit_gate VARCHAR(64),
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_express_manifest_comm_status
    ON express_delivery_manifests (community_id, status);

CREATE INDEX IF NOT EXISTS idx_express_manifest_code
    ON express_delivery_manifests (manifest_code);

ALTER TABLE express_delivery_passes
    ADD COLUMN IF NOT EXISTS manifest_id BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_outcome VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_express_pass_manifest
    ON express_delivery_passes (manifest_id);
