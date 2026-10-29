-- Phase 5: Follow-up reminder columns on CRM leads
ALTER TABLE cpos_crm_lead
    ADD COLUMN follow_up_at   TIMESTAMP NULL,
    ADD COLUMN follow_up_note VARCHAR(500) NULL;

CREATE INDEX idx_cpos_crm_lead_followup ON cpos_crm_lead (follow_up_at)
    WHERE follow_up_at IS NOT NULL AND is_deleted = FALSE;
