-- Mana Fresh: hyperlocal grocery/produce delivery module
CREATE TABLE IF NOT EXISTS fresh_product_category (
    id          BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_fpc_community FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_fpc_community ON fresh_product_category(community_id);

CREATE TABLE IF NOT EXISTS fresh_product (
    id          BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    category_id BIGINT REFERENCES fresh_product_category(id) ON DELETE SET NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    unit        VARCHAR(30),
    price       NUMERIC(10,2) NOT NULL DEFAULT 0,
    stock       INTEGER NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_fp_community FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_fp_community ON fresh_product(community_id);
CREATE INDEX IF NOT EXISTS idx_fp_category  ON fresh_product(category_id);

CREATE TABLE IF NOT EXISTS fresh_order (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    resident_id     BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    delivery_address TEXT,
    delivery_slot   VARCHAR(100),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_fo_community FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_fo_community  ON fresh_order(community_id);
CREATE INDEX IF NOT EXISTS idx_fo_resident   ON fresh_order(resident_id);

CREATE TABLE IF NOT EXISTS fresh_order_item (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES fresh_order(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES fresh_product(id) ON DELETE RESTRICT,
    quantity    INTEGER NOT NULL DEFAULT 1,
    unit_price  NUMERIC(10,2) NOT NULL,
    subtotal    NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_foi_order   ON fresh_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_foi_product ON fresh_order_item(product_id);
