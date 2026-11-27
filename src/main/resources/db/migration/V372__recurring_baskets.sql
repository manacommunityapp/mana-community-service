CREATE TYPE basket_frequency AS ENUM ('DAILY', 'WEEKLY', 'FORTNIGHTLY', 'MONTHLY');
CREATE TYPE basket_status AS ENUM ('ACTIVE', 'PAUSED', 'CANCELLED');

CREATE TABLE recurring_basket (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    vendor_id       BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    frequency       basket_frequency NOT NULL,
    next_order_date DATE NOT NULL,
    slot_template_id BIGINT REFERENCES delivery_slot_template(id),
    status          basket_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE recurring_basket_item (
    id              BIGSERIAL PRIMARY KEY,
    basket_id       BIGINT NOT NULL REFERENCES recurring_basket(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL,
    product_type    VARCHAR(30) NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12,2) NOT NULL,
    accept_substitutes BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_rb_user ON recurring_basket(user_id, status);
CREATE INDEX idx_rb_next ON recurring_basket(next_order_date, status) WHERE status = 'ACTIVE';
