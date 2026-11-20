CREATE TABLE product_stock_status (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    vendor_id       BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_type    VARCHAR(30) NOT NULL,
    is_in_stock     BOOLEAN NOT NULL DEFAULT TRUE,
    restock_date    DATE,
    updated_by      BIGINT,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(vendor_id, product_id, product_type)
);

CREATE TABLE product_substitute (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    vendor_id       BIGINT NOT NULL,
    original_product_id   BIGINT NOT NULL,
    original_product_type VARCHAR(30) NOT NULL,
    substitute_product_id BIGINT NOT NULL,
    substitute_product_type VARCHAR(30) NOT NULL,
    priority        INT NOT NULL DEFAULT 1,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pss_vendor ON product_stock_status(vendor_id, is_in_stock);
CREATE INDEX idx_psub_orig  ON product_substitute(vendor_id, original_product_id, original_product_type);
