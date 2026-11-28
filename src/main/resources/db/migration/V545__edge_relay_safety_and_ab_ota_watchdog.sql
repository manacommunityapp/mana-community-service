-- Edge Gate Controller Relay Pulse Safety & A/B Partition OTA Watchdog Schema

CREATE TABLE IF NOT EXISTS edge_barrier_command_nonces (
    id BIGSERIAL PRIMARY KEY,
    controller_id VARCHAR(64) NOT NULL,
    cmd_nonce VARCHAR(128) NOT NULL,
    command_type VARCHAR(24) NOT NULL, -- OPEN_ONLY, CLOSE
    loop_a_occupied BOOLEAN NOT NULL,
    loop_b_occupied BOOLEAN NOT NULL,
    outcome VARCHAR(40) NOT NULL, -- RELAY_PULSED, IDEMPOTENT_DUPLICATE, STALE_COMMAND_EXPIRED, NO_VEHICLE_ON_LOOP_A, LOOP_B_SAFETY_INHIBIT
    tailgate_detected BOOLEAN NOT NULL DEFAULT FALSE,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_edge_controller_cmd_nonce UNIQUE (controller_id, cmd_nonce)
);

CREATE TABLE IF NOT EXISTS edge_gateway_ota_state (
    id BIGSERIAL PRIMARY KEY,
    controller_id VARCHAR(64) NOT NULL UNIQUE,
    community_id BIGINT NOT NULL,
    active_slot VARCHAR(4) NOT NULL DEFAULT 'A',
    target_slot VARCHAR(4) NOT NULL DEFAULT 'A',
    current_version VARCHAR(64) NOT NULL,
    pending_version VARCHAR(64),
    upgrade_available BOOLEAN NOT NULL DEFAULT FALSE,
    boot_count INT NOT NULL DEFAULT 0,
    boot_limit INT NOT NULL DEFAULT 1,
    soc_temp_celsius DOUBLE PRECISION NOT NULL DEFAULT 45.0,
    ota_status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY', -- HEALTHY, TRIAL_BOOTING, ROLLED_BACK, THERMAL_DEFERRED
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
