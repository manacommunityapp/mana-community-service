CREATE TYPE refund_trigger AS ENUM ('OUT_OF_STOCK', 'SUBSTITUTION_REJECTED', 'LATE_DELIVERY', 'QUALITY_ISSUE', 'WRONG_ITEM', 'CANCELLED_BY_VENDOR', 'MANUAL');
CREATE TYPE refund_status AS ENUM ('INITIATED', 'APPROVED', 'PROCESSED', 'REJECTED');

CREATE TABLE order_refund (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    order_id        BIGINT NOT NULL,
    order_type      VARCHAR(30) NOT NULL,
    user_id         BIGINT NOT NULL,
    trigger_type    refund_trigger NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    item_details    TEXT,
    status          refund_status NOT NULL DEFAULT 'INITIATED',
    auto_approved   BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMP,
    processed_at    TIMESTAMP,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE vendor_prep_analytics (
    id              BIGSERIAL PRIMARY KEY,
    vendor_id       BIGINT NOT NULL,
    community_id    BIGINT NOT NULL,
    order_id        BIGINT NOT NULL,
    order_type      VARCHAR(30) NOT NULL,
    order_placed_at TIMESTAMP NOT NULL,
    prep_started_at TIMESTAMP,
    prep_completed_at TIMESTAMP,
    slot_id         BIGINT,
    was_on_time     BOOLEAN,
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refund_comm  ON order_refund(community_id, status);
CREATE INDEX idx_refund_order ON order_refund(order_id, order_type);
CREATE INDEX idx_vpa_vendor   ON vendor_prep_analytics(vendor_id, order_placed_at);
