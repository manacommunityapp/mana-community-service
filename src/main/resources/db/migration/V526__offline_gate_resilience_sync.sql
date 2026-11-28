-- Offline Gate Resilience, School-Bus Rush Reconciliation, and Anti-Replay Controls

ALTER TABLE manacommunity.sync_change_logs
    ADD COLUMN IF NOT EXISTS device_id        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS local_seq        BIGINT,
    ADD COLUMN IF NOT EXISTS pass_nonce       VARCHAR(128),
    ADD COLUMN IF NOT EXISTS conflict_reason  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS offline_override BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE manacommunity.sync_checkpoints
    ADD COLUMN IF NOT EXISTS last_synced_seq BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS device_status   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE IF NOT EXISTS manacommunity.gate_offline_device (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    device_id       VARCHAR(100) NOT NULL,
    gate_name       VARCHAR(100) NOT NULL,
    signing_secret  VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_seq        BIGINT       NOT NULL DEFAULT 0,
    roster_version  BIGINT       NOT NULL DEFAULT 1,
    last_seen_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gate_offline_device UNIQUE (community_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_gate_offline_device_status
    ON manacommunity.gate_offline_device(community_id, status);

CREATE TABLE IF NOT EXISTS manacommunity.gate_entry_audit (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT       NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    device_id           VARCHAR(100) NOT NULL,
    local_seq           BIGINT,
    client_mutation_id  VARCHAR(100) NOT NULL,
    entity_type         VARCHAR(50)  NOT NULL,
    entity_id           BIGINT,
    pass_code           VARCHAR(100),
    pass_nonce          VARCHAR(128),
    gate_name           VARCHAR(100),
    guard_user_id       BIGINT,
    occurred_at         TIMESTAMP    NOT NULL,
    reconciled_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    sync_status         VARCHAR(30)  NOT NULL,
    offline_override    BOOLEAN      NOT NULL DEFAULT FALSE,
    conflict_reason     VARCHAR(500),
    device_signature    VARCHAR(128),
    resolved            BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_by         BIGINT,
    resolved_at         TIMESTAMP,
    resolution_note     VARCHAR(500),
    CONSTRAINT uq_gate_audit_device_mutation UNIQUE (device_id, client_mutation_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_gate_audit_community_pass_nonce
    ON manacommunity.gate_entry_audit(community_id, pass_nonce)
    WHERE pass_nonce IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_gate_audit_anomalies
    ON manacommunity.gate_entry_audit(community_id, sync_status, resolved, reconciled_at DESC);
