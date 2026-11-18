-- V335: Link food_orders and food_grocery_orders to unified payment_order table

ALTER TABLE food_orders
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_food_order_payment_order
    ON food_orders(payment_order_id);

ALTER TABLE food_grocery_orders
    ADD COLUMN IF NOT EXISTS payment_order_id BIGINT REFERENCES payment_order(id);

CREATE INDEX IF NOT EXISTS idx_food_grocery_order_payment_order
    ON food_grocery_orders(payment_order_id);
