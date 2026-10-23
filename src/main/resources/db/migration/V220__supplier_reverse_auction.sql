-- Reverse Auction: extend procurement_rfqs + new auction_bids table

ALTER TABLE procurement_rfqs
    ADD COLUMN is_reverse_auction BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN auction_deadline    TIMESTAMP    NULL,
    ADD COLUMN reserve_price       DECIMAL(14,2) NULL;

CREATE TABLE auction_bids (
    id            BIGSERIAL      PRIMARY KEY,
    rfq_id        BIGINT         NOT NULL,
    supplier_id   BIGINT,
    supplier_name VARCHAR(200)   NOT NULL,
    bid_amount    DECIMAL(14,2)  NOT NULL,
    delivery_days INT,
    notes         VARCHAR(500),
    status        VARCHAR(30)    NOT NULL DEFAULT 'ACTIVE',
    rank_position INT,
    community_id  BIGINT         NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auction_bids_rfq       ON auction_bids (rfq_id);
CREATE INDEX idx_auction_bids_supplier  ON auction_bids (supplier_id);
CREATE INDEX idx_auction_bids_community ON auction_bids (community_id);
