-- Unified Notification Orchestrator & Preferences Schema

CREATE TABLE IF NOT EXISTS manacommunity.notification_preferences (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT        NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    category             VARCHAR(50)   NOT NULL,
    channel              VARCHAR(30)   NOT NULL,
    is_enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    quiet_hours_enabled  BOOLEAN       NOT NULL DEFAULT FALSE,
    quiet_hours_start    VARCHAR(10)   DEFAULT '22:00',
    quiet_hours_end      VARCHAR(10)   DEFAULT '07:00',
    timezone             VARCHAR(50)   DEFAULT 'Asia/Kolkata',
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_cat_chan UNIQUE (user_id, category, channel)
);

CREATE INDEX IF NOT EXISTS idx_notif_pref_user ON manacommunity.notification_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_pref_lookup ON manacommunity.notification_preferences(user_id, category);

CREATE TABLE IF NOT EXISTS manacommunity.notification_audit_logs (
    id                   BIGSERIAL PRIMARY KEY,
    notification_id      VARCHAR(100)  NOT NULL,
    user_id              BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    category             VARCHAR(50)   NOT NULL,
    channel              VARCHAR(30)   NOT NULL,
    priority             VARCHAR(30)   NOT NULL DEFAULT 'NORMAL',
    title                VARCHAR(255)  NOT NULL,
    body                 TEXT,
    status               VARCHAR(50)   NOT NULL,
    error_reason         TEXT,
    provider_message_id  VARCHAR(100),
    metadata_json        TEXT,
    delivered_at         TIMESTAMP,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notif_audit_user ON manacommunity.notification_audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_audit_status ON manacommunity.notification_audit_logs(status);
CREATE INDEX IF NOT EXISTS idx_notif_audit_created ON manacommunity.notification_audit_logs(created_at);

CREATE TABLE IF NOT EXISTS manacommunity.whatsapp_delivery_logs (
    id                   BIGSERIAL PRIMARY KEY,
    recipient_phone      VARCHAR(20)   NOT NULL,
    template_name        VARCHAR(100)  NOT NULL,
    parameters_json      TEXT,
    status               VARCHAR(50)   NOT NULL,
    provider_message_id  VARCHAR(100),
    error_message        TEXT,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wa_log_phone ON manacommunity.whatsapp_delivery_logs(recipient_phone);
