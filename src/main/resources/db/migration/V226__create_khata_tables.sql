-- Khatabook-style ledger: per-party khata for vendor business and personal udhaar

CREATE TABLE khata_parties (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    community_id  BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    khata_type    VARCHAR(20)  NOT NULL COMMENT 'VENDOR or PERSONAL',
    party_type    VARCHAR(20)  NOT NULL COMMENT 'CUSTOMER, SUPPLIER, FRIEND, FAMILY',
    balance       DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT 'Positive = they owe you (GAVE), Negative = you owe them (GOT)',
    total_gave    DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    total_got     DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    notes         VARCHAR(255),
    last_activity DATETIME,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT fk_khata_party_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_khata_parties_user_type ON khata_parties (user_id, khata_type);

CREATE TABLE khata_entries (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_id     BIGINT        NOT NULL,
    kind         VARCHAR(10)   NOT NULL COMMENT 'GAVE or GOT',
    amount       DECIMAL(14,2) NOT NULL,
    note         VARCHAR(255),
    entry_date   DATE          NOT NULL,
    has_receipt  BOOLEAN       NOT NULL DEFAULT FALSE,
    reminder_sent BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME      NOT NULL,
    CONSTRAINT fk_khata_entry_party FOREIGN KEY (party_id) REFERENCES khata_parties(id) ON DELETE CASCADE
);

CREATE INDEX idx_khata_entries_party ON khata_entries (party_id, entry_date DESC);
