-- Phase 8: CRM Email Templates per community
CREATE TABLE cpos_crm_email_template (
    id           BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    name         VARCHAR(100) NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    body         TEXT NOT NULL,
    created_by   VARCHAR(255) NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crm_email_tpl_community FOREIGN KEY (community_id) REFERENCES community (id)
);
CREATE INDEX idx_crm_email_tpl_community ON cpos_crm_email_template (community_id);

-- Seed three starter templates
INSERT INTO cpos_crm_email_template (community_id, name, subject, body, created_by)
SELECT c.id,
       'Welcome & Introduction',
       'Thank you for your interest — {{leadName}}',
       '<p>Dear {{leadName}},</p>\n<p>Thank you for your interest in our properties. We are excited to assist you in finding your perfect home.</p>\n<p>Please feel free to reply to this email or call us at any time. We look forward to connecting with you soon.</p>\n<p>Warm regards,<br/>{{agentName}}</p>',
       'system'
FROM community c
LIMIT 0; -- no seed rows — let each community create their own
