-- V141: Personal Finance P2 Enhancements
-- Adds receipt attachment to transactions and credit card billing cycle fields to accounts.

ALTER TABLE personal_finance_transactions
ADD COLUMN IF NOT EXISTS receipt_url VARCHAR(1000);

ALTER TABLE personal_finance_accounts
ADD COLUMN IF NOT EXISTS billing_day INT,
ADD COLUMN IF NOT EXISTS payment_due_day INT;
