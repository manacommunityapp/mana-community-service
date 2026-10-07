-- V172__helpdesk_ai_closed_loop.sql
-- Closed-Loop AI Helpdesk Workflow: AI Triage, dynamic SLA tracking, resolution verification, and resident confirmation rework loop

ALTER TABLE manacommunity.helpdesk_ticket
    ADD COLUMN IF NOT EXISTS ai_classification_json TEXT,
    ADD COLUMN IF NOT EXISTS urgency_score           INT          DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sla_status              VARCHAR(20)  DEFAULT 'ON_TRACK', -- ON_TRACK | AT_RISK | BREACHED
    ADD COLUMN IF NOT EXISTS resolution_notes        TEXT,
    ADD COLUMN IF NOT EXISTS resolution_proof_url    VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS resolution_code         VARCHAR(10),
    ADD COLUMN IF NOT EXISTS reopen_count            INT          DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_escalated_at       TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_helpdesk_ticket_sla_status ON manacommunity.helpdesk_ticket(sla_status);
CREATE INDEX IF NOT EXISTS idx_helpdesk_ticket_active_sla ON manacommunity.helpdesk_ticket(status, sla_due_at);
