-- V184: Commerce Flash Sales — time-bounded discounted product campaigns

CREATE TABLE IF NOT EXISTS commerce_flash_sale (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    starts_at       TIMESTAMP NOT NULL,
    ends_at         TIMESTAMP NOT NULL,
    discount_type   VARCHAR(20) NOT NULL DEFAULT 'PERCENT',  -- PERCENT | FLAT
    discount_value  NUMERIC(12, 2) NOT NULL,
    max_quantity    INT,                -- NULL = unlimited per item
    status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED | ACTIVE | ENDED | CANCELLED
    created_by      BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_flash_sale_community ON commerce_flash_sale(community_id);
CREATE INDEX IF NOT EXISTS idx_flash_sale_status ON commerce_flash_sale(status);
CREATE INDEX IF NOT EXISTS idx_flash_sale_dates ON commerce_flash_sale(starts_at, ends_at);

CREATE TABLE IF NOT EXISTS commerce_flash_sale_item (
    id              BIGSERIAL PRIMARY KEY,
    flash_sale_id   BIGINT NOT NULL REFERENCES commerce_flash_sale(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES commerce_products(id) ON DELETE CASCADE,
    original_price  NUMERIC(12, 2) NOT NULL,
    flash_price     NUMERIC(12, 2) NOT NULL,
    quantity_limit  INT,               -- per-item stock cap for this sale
    quantity_sold   INT NOT NULL DEFAULT 0,
    UNIQUE (flash_sale_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_flash_item_sale ON commerce_flash_sale_item(flash_sale_id);
CREATE INDEX IF NOT EXISTS idx_flash_item_product ON commerce_flash_sale_item(product_id);
