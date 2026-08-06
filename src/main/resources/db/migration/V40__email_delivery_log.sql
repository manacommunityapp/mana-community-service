-- V40__email_delivery_log.sql
-- Tracks every email dispatch attempt made by SmtpEmailService.
-- Gives admins full visibility into delivery success, failures, and open events
-- without requiring an external email service.

CREATE TABLE IF NOT EXISTS email_delivery_log (
    id            BIGSERIAL PRIMARY KEY,

    -- Envelope
    recipient     VARCHAR(255) NOT NULL,
    subject       VARCHAR(500),
    template_type VARCHAR(100),          -- e.g. OTP, MATCH_REMINDER, TOURNAMENT_START

    -- Outcome
    status        VARCHAR(20)  NOT NULL, -- SENT | FAILED | SKIPPED
    error_message TEXT,                  -- populated on FAILED

    -- Open tracking (Level 2 — populated when recipient loads the tracking pixel)
    tracking_token VARCHAR(64) UNIQUE,   -- UUID stamped into the pixel URL
    opened_at      TIMESTAMP WITHOUT TIME ZONE,

    -- Community context
    community_id  BIGINT REFERENCES communities(id) ON DELETE SET NULL,

    -- Timestamps
    sent_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for admin dashboard queries
CREATE INDEX idx_email_log_status    ON email_delivery_log(status);
CREATE INDEX idx_email_log_recipient ON email_delivery_log(recipient);
CREATE INDEX idx_email_log_sent_at   ON email_delivery_log(sent_at DESC);
CREATE INDEX idx_email_log_community ON email_delivery_log(community_id);
CREATE INDEX idx_email_log_type      ON email_delivery_log(template_type);
CREATE INDEX idx_email_log_token     ON email_delivery_log(tracking_token) WHERE tracking_token IS NOT NULL;
