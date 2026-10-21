-- Portal inbound webhook configuration (one row per community)
CREATE TABLE cpos_crm_inbound_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    community_id BIGINT       NOT NULL,
    api_key      VARCHAR(64)  NOT NULL UNIQUE,
    agent_pool   TEXT         NULL,     -- JSON array of agent names, e.g. ["Ravi Sharma","Priya Nair"]
    rr_index     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_inbound_config_community UNIQUE (community_id),
    CONSTRAINT fk_inbound_config_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE INDEX idx_inbound_config_api_key ON cpos_crm_inbound_config (api_key);
