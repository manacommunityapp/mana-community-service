-- Offline-First Delta Sync & Changelog Schema

CREATE TABLE IF NOT EXISTS manacommunity.sync_change_logs (
    id                   BIGSERIAL PRIMARY KEY,
    entity_type          VARCHAR(50)   NOT NULL, -- VISITOR_PASS, PARKING_ENTRY, SOS_TRIGGER, HELPDESK_TICKET, EV_SESSION
    entity_id            BIGINT,
    community_id         BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    user_id              BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    operation            VARCHAR(20)   NOT NULL, -- CREATE, UPDATE, DELETE
    payload_json         TEXT          NOT NULL,
    client_mutation_id   VARCHAR(100)  NOT NULL UNIQUE,
    client_timestamp     TIMESTAMP     NOT NULL,
    server_timestamp     TIMESTAMP     NOT NULL DEFAULT NOW(),
    version              BIGINT        NOT NULL DEFAULT 1,
    status               VARCHAR(30)   NOT NULL DEFAULT 'APPLIED' -- APPLIED, CONFLICT_SERVER_WINS, REJECTED
);

CREATE INDEX IF NOT EXISTS idx_sync_community_ver ON manacommunity.sync_change_logs(community_id, id);
CREATE INDEX IF NOT EXISTS idx_sync_user_ver ON manacommunity.sync_change_logs(user_id, id);
CREATE INDEX IF NOT EXISTS idx_sync_mutation ON manacommunity.sync_change_logs(client_mutation_id);

CREATE TABLE IF NOT EXISTS manacommunity.sync_checkpoints (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    device_id            VARCHAR(100)  NOT NULL,
    last_synced_change_id BIGINT       NOT NULL DEFAULT 0,
    last_sync_timestamp  TIMESTAMP     NOT NULL DEFAULT NOW(),
    client_app_version   VARCHAR(50),
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_device_sync UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_sync_ckpt_user ON manacommunity.sync_checkpoints(user_id);
