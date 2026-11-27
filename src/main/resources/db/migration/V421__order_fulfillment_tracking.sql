-- Append-only fulfillment event log for all order types
CREATE TABLE IF NOT EXISTS order_fulfillment_events (
    id               BIGSERIAL PRIMARY KEY,
    order_id         VARCHAR(100) NOT NULL,  -- external reference (food, grocery, marketplace, service)
    order_type       VARCHAR(50)  NOT NULL,  -- FOOD, GROCERY, MARKETPLACE, HOME_SERVICE
    community_id     BIGINT       NOT NULL,
    event_type       VARCHAR(50)  NOT NULL,
    -- PLACED, CONFIRMED, PREPARING, READY, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    actor_id         BIGINT,
    actor_role       VARCHAR(50),  -- SELLER, DELIVERY_PARTNER, SYSTEM, CUSTOMER
    note             TEXT,
    lat              NUMERIC(9,6),
    lng              NUMERIC(9,6),
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS delivery_assignments (
    id                     BIGSERIAL PRIMARY KEY,
    order_id               VARCHAR(100) NOT NULL,
    community_id           BIGINT       NOT NULL,
    delivery_person_id     BIGINT,
    delivery_person_name   VARCHAR(200),
    delivery_person_phone  VARCHAR(20),
    estimated_arrival      TIMESTAMPTZ,
    actual_delivery        TIMESTAMPTZ,
    status                 VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    -- ASSIGNED, EN_ROUTE, DELIVERED, FAILED
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ofe_order     ON order_fulfillment_events(order_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ofe_community ON order_fulfillment_events(community_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_da_order      ON delivery_assignments(order_id);
