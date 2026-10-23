-- Accountability Alert
CREATE TABLE manacommunity.accountability_alert (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    partner_user_id BIGINT,
    alert_type VARCHAR(30) NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    message VARCHAR(500),
    data_json JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_accountability_alert_user ON manacommunity.accountability_alert(user_id, triggered_at DESC);
CREATE INDEX idx_accountability_alert_unread ON manacommunity.accountability_alert(user_id, is_read) WHERE is_read = FALSE;

-- Add junk_food_score to meal_log for persistence
ALTER TABLE manacommunity.meal_log ADD COLUMN IF NOT EXISTS junk_food_score NUMERIC(4,2);
ALTER TABLE manacommunity.meal_log ADD COLUMN IF NOT EXISTS is_junk_food BOOLEAN DEFAULT FALSE;

-- Add junk_food_score to ai analysis fields on meal_log
COMMENT ON COLUMN manacommunity.meal_log.junk_food_score IS '0.0-1.0 junk food probability from AI analysis';
