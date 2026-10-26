-- Add call outcome tracking to CRM activity log
ALTER TABLE cpos_crm_activity
    ADD COLUMN call_outcome     VARCHAR(20)  NULL,  -- CONNECTED | MISSED | BUSY
    ADD COLUMN call_duration_min INT          NULL;  -- call duration in minutes (null for non-call activities)

CREATE INDEX idx_cpos_crm_activity_call ON cpos_crm_activity (activity_type, call_outcome)
    WHERE activity_type = 'CALL';
