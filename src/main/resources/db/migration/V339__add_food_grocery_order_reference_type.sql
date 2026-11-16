-- V339: Add FOOD_GROCERY_ORDER to the payment_reference_type PostgreSQL enum.
-- PostgreSQL requires ALTER TYPE ... ADD VALUE for enum extensions (not in a transaction).
ALTER TYPE payment_reference_type ADD VALUE IF NOT EXISTS 'FOOD_GROCERY_ORDER' BEFORE 'MARKETPLACE_ORDER';
