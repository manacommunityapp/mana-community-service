CREATE TABLE IF NOT EXISTS manacommunity.trip_p2p_collect_request (
    id                   BIGSERIAL PRIMARY KEY,
    community_id         BIGINT        NOT NULL,
    trip_id              VARCHAR(50)   NOT NULL REFERENCES manacommunity.community_trip(id),
    creditor_user_id     BIGINT        NOT NULL,
    debtor_user_id       BIGINT        NOT NULL,
    amount               NUMERIC(14,2) NOT NULL,
    upi_vpa              VARCHAR(100),
    status               VARCHAR(30)   NOT NULL DEFAULT 'PENDING_VPA',
    razorpay_payment_id  VARCHAR(100),
    razorpay_order_id    VARCHAR(100),
    description          VARCHAR(255),
    initiated_at         TIMESTAMP,
    settled_at           TIMESTAMP,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_p2p_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_p2p_collect_trip      ON manacommunity.trip_p2p_collect_request(trip_id);
CREATE INDEX IF NOT EXISTS idx_p2p_collect_debtor    ON manacommunity.trip_p2p_collect_request(debtor_user_id);
CREATE INDEX IF NOT EXISTS idx_p2p_collect_creditor  ON manacommunity.trip_p2p_collect_request(creditor_user_id);
CREATE INDEX IF NOT EXISTS idx_p2p_collect_status    ON manacommunity.trip_p2p_collect_request(community_id, status);
