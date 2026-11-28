-- V548: Security alert table for gate HOLD events

CREATE TABLE security_alert (
    id               BIGSERIAL    PRIMARY KEY,
    community_id     BIGINT       NOT NULL,
    gate_id          VARCHAR(50)  NOT NULL,
    gate_event_id    BIGINT,
    hardware_type    VARCHAR(20),
    identifier_type  VARCHAR(20),
    identifier_value VARCHAR(100),
    alert_status     VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    acknowledged_by  VARCHAR(100),
    acknowledged_at  TIMESTAMP,
    resolved_by      VARCHAR(100),
    resolved_at      TIMESTAMP,
    resolution_notes TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_security_alert_community  FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_security_alert_gate_event FOREIGN KEY (gate_event_id) REFERENCES gate_event (id)
);

CREATE INDEX idx_security_alert_community ON security_alert (community_id);
CREATE INDEX idx_security_alert_status    ON security_alert (alert_status);
CREATE INDEX idx_security_alert_created   ON security_alert (created_at DESC);
