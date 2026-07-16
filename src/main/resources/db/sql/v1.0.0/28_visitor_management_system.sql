-- FILE: db/sql/v1.0.0/28_visitor_management_system.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- VISITOR MANAGEMENT SYSTEM — Phase 2: Enhanced Columns & Audit Logs
-- ═══════════════════════════════════════════════════════════════════════════

-- Update visitor_pass columns
ALTER TABLE manacommunity.visitor_pass 
    ADD COLUMN IF NOT EXISTS otp VARCHAR(6),
    ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS gate_in VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gate_out VARCHAR(100),
    ADD COLUMN IF NOT EXISTS guard_in VARCHAR(100),
    ADD COLUMN IF NOT EXISTS guard_out VARCHAR(100),
    ADD COLUMN IF NOT EXISTS visitor_photo TEXT,
    ADD COLUMN IF NOT EXISTS encrypted_token VARCHAR(500);

-- Create visitor_audit_log table
CREATE TABLE IF NOT EXISTS manacommunity.visitor_audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    visitor_pass_id BIGINT          NOT NULL,
    visitor_name    VARCHAR(100)    NOT NULL,
    action          VARCHAR(50)     NOT NULL, -- CREATED, OTP_GENERATED, RESIDENT_APPROVED, ENTRY_COMPLETED, EXIT_COMPLETED, REJECTED
    performed_by    VARCHAR(100)    NOT NULL, -- e.g. "Resident (AppUser Name)", "Guard (Guard Name)", "System"
    timestamp       TIMESTAMP       NOT NULL DEFAULT NOW(),
    details         TEXT,
    FOREIGN KEY (visitor_pass_id) REFERENCES manacommunity.visitor_pass(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_visitor_audit_pass ON manacommunity.visitor_audit_log(visitor_pass_id);
