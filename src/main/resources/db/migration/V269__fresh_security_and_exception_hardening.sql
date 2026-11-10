-- =============================================================================
-- Migration: V269__fresh_security_and_exception_hardening.sql
-- Description: Mana Fresh Security & Exception Hardening:
--              Add brute-force OTP lockout tracking to fresh_fulfilment_group
-- Schema: manacommunity
-- =============================================================================

ALTER TABLE manacommunity.fresh_fulfilment_group
    ADD COLUMN IF NOT EXISTS failed_otp_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_handover_locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_group_lockout
    ON manacommunity.fresh_fulfilment_group(is_handover_locked, status);
