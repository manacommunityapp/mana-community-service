-- V258: Link Group Buy Orders to Commerce Orders, Add Quantity Reservation and Concurrency Controls

ALTER TABLE manacommunity.group_buy_deal
    ADD COLUMN IF NOT EXISTS reserved_qty INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE manacommunity.group_buy_order
    ADD COLUMN IF NOT EXISTS commerce_order_id BIGINT REFERENCES manacommunity.commerce_order(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reserved_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS legacy_unverified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_group_buy_order_commerce_order ON manacommunity.group_buy_order(commerce_order_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_order_reserved ON manacommunity.group_buy_order(status, reserved_until);
CREATE INDEX IF NOT EXISTS idx_group_buy_deal_lifecycle ON manacommunity.group_buy_deal(deal_status, deal_ends_at);
