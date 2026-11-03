-- V262: Add JPA optimistic-lock version column to commerce_wallet
-- Prevents concurrent wallet debit double-spend by letting Hibernate
-- detect mid-flight concurrent updates and raise OptimisticLockException.

ALTER TABLE manacommunity.commerce_wallet
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
