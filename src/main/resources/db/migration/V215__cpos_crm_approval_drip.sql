-- Approval workflow columns on lead
ALTER TABLE cpos_crm_lead
    ADD COLUMN approval_status  VARCHAR(30)  NULL,
    ADD COLUMN approval_reason  VARCHAR(500) NULL,
    ADD COLUMN approval_comment VARCHAR(500) NULL;

-- Email drip sequences
CREATE TABLE cpos_crm_drip_sequence (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    trigger_stage VARCHAR(50) NULL,
    active       TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT fk_drip_seq_community FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE
);

CREATE TABLE cpos_crm_drip_step (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sequence_id  BIGINT       NOT NULL,
    delay_days   INT          NOT NULL DEFAULT 0,
    subject      VARCHAR(500) NOT NULL,
    body_html    LONGTEXT     NULL,
    CONSTRAINT fk_drip_step_seq FOREIGN KEY (sequence_id) REFERENCES cpos_crm_drip_sequence(id) ON DELETE CASCADE
);
