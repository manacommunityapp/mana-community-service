-- V100: Gate hardware adapter — unified event log and RFID tag registry

CREATE TABLE gate_event (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT       NOT NULL,
    gate_id             VARCHAR(50)  NOT NULL,
    hardware_type       VARCHAR(20)  NOT NULL,
    direction           VARCHAR(10)  NOT NULL DEFAULT 'ENTRY',
    identifier_type     VARCHAR(20),
    identifier_value    VARCHAR(100),
    matched_entity_type VARCHAR(20),
    matched_entity_id   BIGINT,
    matched_name        VARCHAR(100),
    barrier_action      VARCHAR(10)  NOT NULL DEFAULT 'HOLD',
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_gate_event_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE INDEX idx_gate_event_community  ON gate_event (community_id);
CREATE INDEX idx_gate_event_gate_id    ON gate_event (gate_id);
CREATE INDEX idx_gate_event_created_at ON gate_event (created_at DESC);

CREATE TABLE rfid_tag_mapping (
    id            BIGSERIAL    PRIMARY KEY,
    community_id  BIGINT       NOT NULL,
    rfid_tag_id   VARCHAR(100) NOT NULL,
    flat_number   VARCHAR(30),
    resident_id   BIGINT,
    resident_name VARCHAR(100),
    vehicle_type  VARCHAR(20)  DEFAULT 'CAR',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rfid_tag_community UNIQUE (community_id, rfid_tag_id),
    CONSTRAINT fk_rfid_mapping_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE INDEX idx_rfid_community ON rfid_tag_mapping (community_id);
