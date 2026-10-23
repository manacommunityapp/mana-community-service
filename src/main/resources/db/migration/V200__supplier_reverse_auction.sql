-- Reverse Auction: extend procurement_rfqs + new auction_bids table

ALTER TABLE procurement_rfqs
    ADD COLUMN is_reverse_auction BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN auction_deadline    TIMESTAMP NULL,
    ADD COLUMN reserve_price       DECIMAL(14,2) NULL;

CREATE TABLE auction_bids (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_id        BIGINT        NOT NULL,
    supplier_id   BIGINT,
    supplier_name VARCHAR(200)  NOT NULL,
    bid_amount    DECIMAL(14,2) NOT NULL,
    delivery_days INT,
    notes         VARCHAR(500),
    status        VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    rank_position INT,
    community_id  BIGINT        NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_auction_bids_rfq       (rfq_id),
    INDEX idx_auction_bids_supplier  (supplier_id),
    INDEX idx_auction_bids_community (community_id)
);
