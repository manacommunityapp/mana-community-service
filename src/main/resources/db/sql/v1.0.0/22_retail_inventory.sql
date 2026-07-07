-- FILE: db/sql/v1.0.0/22_retail_inventory.sql
-- Retail inventory module tables: products, suppliers, customers, orders, order lines.
-- Community-scoped. Idempotent.

CREATE TABLE IF NOT EXISTS retail_products (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(120)    NOT NULL,
    emoji           VARCHAR(10),
    category        VARCHAR(60),
    unit_price      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    reorder_level   INT             NOT NULL DEFAULT 10,
    units_ordered   INT             NOT NULL DEFAULT 0,
    units_sold      INT             NOT NULL DEFAULT 0,
    community_id    BIGINT          REFERENCES communities(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS retail_suppliers (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(120)    NOT NULL,
    contact_person  VARCHAR(120),
    phone           VARCHAR(30),
    email           VARCHAR(120),
    community_id    BIGINT          REFERENCES communities(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS retail_customers (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(120)    NOT NULL,
    email           VARCHAR(120),
    phone           VARCHAR(30),
    community_id    BIGINT          REFERENCES communities(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS retail_orders (
    id              BIGSERIAL       PRIMARY KEY,
    code            VARCHAR(20)     NOT NULL UNIQUE,
    order_type      VARCHAR(10)     NOT NULL,
    party_id        BIGINT          NOT NULL,
    order_date      DATE            NOT NULL,
    status          VARCHAR(12)     NOT NULL DEFAULT 'OPEN',
    community_id    BIGINT          REFERENCES communities(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS retail_order_lines (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES retail_orders(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL,
    qty             INT             NOT NULL,
    unit_price      NUMERIC(12,2)   NOT NULL
);
