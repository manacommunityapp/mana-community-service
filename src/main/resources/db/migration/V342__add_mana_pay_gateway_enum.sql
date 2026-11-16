-- Add MANA_PAY to the payment_gateway PostgreSQL ENUM
-- (created in V331__unified_payment_order.sql)
ALTER TYPE payment_gateway ADD VALUE IF NOT EXISTS 'MANA_PAY';
