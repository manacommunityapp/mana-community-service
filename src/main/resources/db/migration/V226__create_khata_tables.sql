-- Khatabook-style ledger: per-party khata for vendor business and personal udhaar

CREATE TABLE khata_parties (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    community_id  BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    khata_type    VARCHAR(20)  NOT NULL,
    party_type    VARCHAR(20)  NOT NULL,
    balance       DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    total_gave    DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    total_got     DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    notes         VARCHAR(255),
    last_activity TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT fk_khata_party_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_khata_parties_user_type ON khata_parties (user_id, khata_type);

CREATE TABLE khata_entries (
    id            BIGSERIAL     PRIMARY KEY,
    party_id      BIGINT        NOT NULL,
    kind          VARCHAR(10)   NOT NULL,
    amount        DECIMAL(14,2) NOT NULL,
    note          VARCHAR(255),
    entry_date    DATE          NOT NULL,
    has_receipt   BOOLEAN       NOT NULL DEFAULT FALSE,
    reminder_sent BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_khata_entry_party FOREIGN KEY (party_id) REFERENCES khata_parties(id) ON DELETE CASCADE
);

CREATE INDEX idx_khata_entries_party ON khata_entries (party_id, entry_date DESC);
