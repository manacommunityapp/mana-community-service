CREATE TYPE cold_chain_type AS ENUM ('AMBIENT', 'CHILLED', 'FROZEN');

CREATE TABLE delivery_batch (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    vendor_id       BIGINT NOT NULL,
    slot_id         BIGINT NOT NULL REFERENCES delivery_slot(id),
    tower_block     VARCHAR(50),
    batch_sequence  INT NOT NULL DEFAULT 1,
    order_count     INT NOT NULL DEFAULT 0,
    assigned_to     BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE delivery_batch_order (
    id              BIGSERIAL PRIMARY KEY,
    batch_id        BIGINT NOT NULL REFERENCES delivery_batch(id),
    order_id        BIGINT NOT NULL,
    order_type      VARCHAR(30) NOT NULL,
    flat_number     VARCHAR(20) NOT NULL,
    cold_chain_type cold_chain_type NOT NULL DEFAULT 'AMBIENT',
    delivered_at    TIMESTAMP,
    delivery_notes  TEXT
);

CREATE TABLE product_cold_chain (
    id              BIGSERIAL PRIMARY KEY,
    vendor_id       BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_type    VARCHAR(30) NOT NULL,
    cold_chain_type cold_chain_type NOT NULL DEFAULT 'CHILLED',
    UNIQUE(vendor_id, product_id, product_type)
);

CREATE INDEX idx_db_slot   ON delivery_batch(slot_id, tower_block);
CREATE INDEX idx_dbo_batch ON delivery_batch_order(batch_id);
CREATE INDEX idx_dbo_order ON delivery_batch_order(order_id, order_type);
