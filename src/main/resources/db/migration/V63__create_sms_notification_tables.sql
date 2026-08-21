-- V63: Centralized SMS Notification Service tables
-- All tables scoped to the manacommunity schema (set by Flyway).

-- ─── sms_template ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_template (
    id                  BIGSERIAL PRIMARY KEY,
    template_code       VARCHAR(80)  NOT NULL,
    template_name       VARCHAR(160) NOT NULL,
    message_type        VARCHAR(40)  NOT NULL,   -- TRANSACTIONAL | OTP | EVENT | PAYMENT ...
    language            VARCHAR(10)  NOT NULL DEFAULT 'EN',
    content             TEXT         NOT NULL,
    allowed_variables   TEXT,                    -- JSON array of permitted placeholder keys
    provider_template_id VARCHAR(120),           -- DLT / MSG91 / Twilio template ID
    sender_id           VARCHAR(30),             -- Sender ID per DLT registration
    version             INT          NOT NULL DEFAULT 1,
    status              VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    created_by          BIGINT,
    approved_by         BIGINT,
    approved_at         TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sms_template_code_lang UNIQUE (template_code, language, version)
);
CREATE INDEX IF NOT EXISTS idx_sms_template_code   ON sms_template(template_code);
CREATE INDEX IF NOT EXISTS idx_sms_template_status ON sms_template(status);

-- ─── sms_message ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_message (
    id                   BIGSERIAL    PRIMARY KEY,
    message_reference    VARCHAR(40)  NOT NULL UNIQUE,
    community_id         BIGINT,
    user_id              BIGINT,
    event_id             BIGINT,
    business_type        VARCHAR(60),
    business_id          VARCHAR(80),
    phone_number         VARCHAR(20)  NOT NULL,
    masked_phone_number  VARCHAR(20)  NOT NULL,
    template_id          BIGINT       REFERENCES sms_template(id),
    template_code        VARCHAR(80),
    message_type         VARCHAR(40)  NOT NULL DEFAULT 'TRANSACTIONAL',
    language             VARCHAR(10)  NOT NULL DEFAULT 'EN',
    priority             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    message_body         TEXT         NOT NULL,
    rendered_body        TEXT,
    provider             VARCHAR(40),
    provider_message_id  VARCHAR(120),
    status               VARCHAR(30)  NOT NULL DEFAULT 'CREATED',
    retry_count          INT          NOT NULL DEFAULT 0,
    max_retries          INT          NOT NULL DEFAULT 3,
    failure_code         VARCHAR(60),
    failure_reason       VARCHAR(500),
    idempotency_key      VARCHAR(120) UNIQUE,
    scheduled_at         TIMESTAMP,
    queued_at            TIMESTAMP,
    sent_at              TIMESTAMP,
    delivered_at         TIMESTAMP,
    failed_at            TIMESTAMP,
    next_retry_at        TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sms_msg_reference    ON sms_message(message_reference);
CREATE INDEX IF NOT EXISTS idx_sms_msg_user_id      ON sms_message(user_id);
CREATE INDEX IF NOT EXISTS idx_sms_msg_community_id ON sms_message(community_id);
CREATE INDEX IF NOT EXISTS idx_sms_msg_event_id     ON sms_message(event_id);
CREATE INDEX IF NOT EXISTS idx_sms_msg_status       ON sms_message(status);
CREATE INDEX IF NOT EXISTS idx_sms_msg_provider_mid ON sms_message(provider_message_id);
CREATE INDEX IF NOT EXISTS idx_sms_msg_scheduled_at ON sms_message(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_sms_msg_next_retry   ON sms_message(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_sms_msg_created_at   ON sms_message(created_at);

-- ─── sms_delivery_event (webhook idempotency) ────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_delivery_event (
    id                   BIGSERIAL    PRIMARY KEY,
    provider             VARCHAR(40)  NOT NULL,
    provider_event_id    VARCHAR(120) NOT NULL,
    provider_message_id  VARCHAR(120),
    event_type           VARCHAR(60),
    payload              TEXT,
    received_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at         TIMESTAMP,
    status               VARCHAR(30)  NOT NULL DEFAULT 'RECEIVED',
    CONSTRAINT uk_sms_delivery_event UNIQUE (provider, provider_event_id)
);
CREATE INDEX IF NOT EXISTS idx_sms_del_evt_provider_mid ON sms_delivery_event(provider_message_id);

-- ─── sms_otp_transaction ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_otp_transaction (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT,
    phone_number   VARCHAR(20)  NOT NULL,
    purpose        VARCHAR(40)  NOT NULL,   -- LOGIN | REGISTRATION | PASSWORD_RESET | PHONE_VERIFY
    otp_hash       VARCHAR(100) NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    attempt_count  INT          NOT NULL DEFAULT 0,
    max_attempts   INT          NOT NULL DEFAULT 5,
    status         VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    verified_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sms_otp_phone   ON sms_otp_transaction(phone_number);
CREATE INDEX IF NOT EXISTS idx_sms_otp_user_id ON sms_otp_transaction(user_id);
CREATE INDEX IF NOT EXISTS idx_sms_otp_status  ON sms_otp_transaction(status);

-- ─── user_sms_preference ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_sms_preference (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT    NOT NULL UNIQUE,
    sms_enabled           BOOLEAN   NOT NULL DEFAULT TRUE,
    event_sms_enabled     BOOLEAN   NOT NULL DEFAULT TRUE,
    payment_sms_enabled   BOOLEAN   NOT NULL DEFAULT TRUE,
    volunteer_sms_enabled BOOLEAN   NOT NULL DEFAULT TRUE,
    marketing_sms_enabled BOOLEAN   NOT NULL DEFAULT FALSE,
    emergency_sms_enabled BOOLEAN   NOT NULL DEFAULT TRUE,
    preferred_language    VARCHAR(10) NOT NULL DEFAULT 'EN',
    phone_verified        BOOLEAN   NOT NULL DEFAULT FALSE,
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_user_sms_pref_user_id ON user_sms_preference(user_id);

-- ─── sms_cost_record ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_cost_record (
    id               BIGSERIAL      PRIMARY KEY,
    sms_message_id   BIGINT         REFERENCES sms_message(id),
    provider         VARCHAR(40)    NOT NULL,
    message_units    INT            NOT NULL DEFAULT 1,
    cost_per_unit    NUMERIC(10,6)  NOT NULL DEFAULT 0,
    total_cost       NUMERIC(12,4)  NOT NULL DEFAULT 0,
    currency         VARCHAR(5)     NOT NULL DEFAULT 'INR',
    community_id     BIGINT,
    event_id         BIGINT,
    message_type     VARCHAR(40),
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sms_cost_msg_id      ON sms_cost_record(sms_message_id);
CREATE INDEX IF NOT EXISTS idx_sms_cost_community   ON sms_cost_record(community_id);
CREATE INDEX IF NOT EXISTS idx_sms_cost_created_at  ON sms_cost_record(created_at);

-- ─── bulk_sms_campaign ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bulk_sms_campaign (
    id                BIGSERIAL    PRIMARY KEY,
    campaign_reference VARCHAR(40) NOT NULL UNIQUE,
    community_id      BIGINT,
    event_id          BIGINT,
    template_code     VARCHAR(80)  NOT NULL,
    language          VARCHAR(10)  NOT NULL DEFAULT 'EN',
    variables         TEXT,                    -- JSON object of template variables
    recipient_filter  TEXT,                    -- JSON: {type, communityId, eventId, ...}
    recipient_count   INT          NOT NULL DEFAULT 0,
    sent_count        INT          NOT NULL DEFAULT 0,
    failed_count      INT          NOT NULL DEFAULT 0,
    status            VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    created_by        BIGINT,
    approved_by       BIGINT,
    approved_at       TIMESTAMP,
    scheduled_at      TIMESTAMP,
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bulk_campaign_reference  ON bulk_sms_campaign(campaign_reference);
CREATE INDEX IF NOT EXISTS idx_bulk_campaign_community  ON bulk_sms_campaign(community_id);
CREATE INDEX IF NOT EXISTS idx_bulk_campaign_status     ON bulk_sms_campaign(status);

-- ─── Seed initial SMS templates ───────────────────────────────────────────────
INSERT INTO sms_template (template_code, template_name, message_type, language, content, allowed_variables, status, version)
SELECT code, name, msg_type, lang, body, vars, 'ACTIVE', 1 FROM (VALUES
  ('REGISTRATION_CONFIRMED',   'Registration Confirmed',       'EVENT',         'EN', 'Dear {{name}}, your registration for {{eventName}} is confirmed. Reg ID: {{registrationId}}. Date: {{eventDate}}.', '["name","eventName","registrationId","eventDate","venue"]', 'ACTIVE'),
  ('REGISTRATION_APPROVED',    'Registration Approved',        'EVENT',         'EN', 'Dear {{name}}, your registration for {{eventName}} has been approved. See you on {{eventDate}}!', '["name","eventName","eventDate"]', 'ACTIVE'),
  ('REGISTRATION_REJECTED',    'Registration Rejected',        'EVENT',         'EN', 'Dear {{name}}, your registration for {{eventName}} was not approved. Contact your community admin for details.', '["name","eventName"]', 'ACTIVE'),
  ('EVENT_REMINDER_24H',       'Event Reminder 24H',           'EVENT',         'EN', 'Reminder: {{eventName}} is tomorrow at {{eventTime}} at {{venue}}. See you there!', '["name","eventName","eventTime","venue","eventDate"]', 'ACTIVE'),
  ('EVENT_REMINDER_1H',        'Event Reminder 1H',            'EVENT',         'EN', 'Heads up! {{eventName}} starts in 1 hour at {{venue}}. Get ready!', '["name","eventName","eventTime","venue"]', 'ACTIVE'),
  ('EVENT_UPDATED',            'Event Updated',                'EVENT',         'EN', 'Dear {{name}}, {{eventName}} has been updated. New date: {{eventDate}}, Venue: {{venue}}.', '["name","eventName","eventDate","venue"]', 'ACTIVE'),
  ('EVENT_CANCELLED',          'Event Cancelled',              'EVENT',         'EN', 'Dear {{name}}, {{eventName}} scheduled for {{eventDate}} has been cancelled. We apologize for the inconvenience.', '["name","eventName","eventDate"]', 'ACTIVE'),
  ('PAYMENT_SUCCESS',          'Payment Successful',           'PAYMENT',       'EN', 'Payment of Rs.{{amount}} for {{eventName}} successful. Ref: {{paymentReference}}.', '["name","amount","eventName","paymentReference"]', 'ACTIVE'),
  ('PAYMENT_FAILED',           'Payment Failed',               'PAYMENT',       'EN', 'Payment of Rs.{{amount}} for {{eventName}} failed. Please retry. Ref: {{paymentReference}}.', '["name","amount","eventName","paymentReference"]', 'ACTIVE'),
  ('PAYMENT_REFUND',           'Payment Refund',               'PAYMENT',       'EN', 'Refund of Rs.{{amount}} for {{eventName}} initiated. It will reflect in 5-7 business days.', '["name","amount","eventName","paymentReference"]', 'ACTIVE'),
  ('POOJA_BOOKED',             'Pooja Booking Confirmed',      'EVENT',         'EN', 'Dear {{name}}, your {{poojaName}} booking is confirmed for {{eventDate}} at {{eventTime}}. Booking ID: {{bookingId}}.', '["name","poojaName","eventDate","eventTime","bookingId"]', 'ACTIVE'),
  ('POOJA_REMINDER',           'Pooja Reminder',               'EVENT',         'EN', 'Reminder: {{poojaName}} is tomorrow at {{eventTime}}. Booking ID: {{bookingId}}.', '["name","poojaName","eventDate","eventTime","bookingId"]', 'ACTIVE'),
  ('CULTURAL_REGISTERED',      'Cultural Activity Registered', 'EVENT',         'EN', 'Dear {{name}}, you have been registered for {{activityName}} on {{eventDate}}.', '["name","activityName","eventDate","eventTime"]', 'ACTIVE'),
  ('CULTURAL_APPROVED',        'Cultural Activity Approved',   'EVENT',         'EN', '{{participantName}} has been approved for {{activityName}} on {{eventDate}} at {{eventTime}}.', '["participantName","activityName","eventDate","eventTime"]', 'ACTIVE'),
  ('CULTURAL_REJECTED',        'Cultural Activity Rejected',   'EVENT',         'EN', 'Dear {{name}}, your application for {{activityName}} was not approved this time.', '["name","activityName","eventDate"]', 'ACTIVE'),
  ('DONATION_SUCCESS',         'Donation Thank You',           'PAYMENT',       'EN', 'Thank you {{name}} for your contribution of Rs.{{amount}} towards {{category}}. Receipt: {{receiptNumber}}.', '["name","amount","category","receiptNumber"]', 'ACTIVE'),
  ('AUCTION_WON',              'Auction Won',                  'EVENT',         'EN', 'Congratulations {{name}}! You won {{itemName}} for Rs.{{amount}}. Complete payment before {{deadline}}.', '["name","itemName","amount","deadline"]', 'ACTIVE'),
  ('AUCTION_PAYMENT_REMINDER', 'Auction Payment Reminder',     'EVENT',         'EN', 'Reminder {{name}}: Payment of Rs.{{amount}} for {{itemName}} is due by {{deadline}}.', '["name","itemName","amount","deadline"]', 'ACTIVE'),
  ('FOOD_BOOKING_CONFIRMED',   'Food Booking Confirmed',       'EVENT',         'EN', 'Dear {{name}}, your food booking for {{eventName}} is confirmed. Pass ID: {{passId}}.', '["name","eventName","passId"]', 'ACTIVE'),
  ('VOLUNTEER_ASSIGNED',       'Volunteer Assigned',           'EVENT',         'EN', 'Dear {{name}}, you have been assigned as volunteer for {{eventName}} on {{eventDate}} at {{venueName}}.', '["name","eventName","eventDate","venueName"]', 'ACTIVE'),
  ('VOLUNTEER_REMINDER',       'Volunteer Reminder',           'EVENT',         'EN', 'Volunteer Reminder: {{eventName}} is tomorrow. Your shift: {{shiftTime}} at {{venueName}}.', '["name","eventName","shiftTime","venueName"]', 'ACTIVE'),
  ('OTP_LOGIN',                'OTP Login',                    'OTP',           'EN', 'Your Mana Community login OTP is {{otp}}. Valid for {{expiryMinutes}} minutes. Do not share.', '["otp","expiryMinutes"]', 'ACTIVE'),
  ('OTP_REGISTRATION',         'OTP Registration',             'OTP',           'EN', 'Your Mana Community registration OTP is {{otp}}. Valid for {{expiryMinutes}} minutes. Do not share.', '["otp","expiryMinutes"]', 'ACTIVE'),
  ('OTP_PASSWORD_RESET',       'OTP Password Reset',           'OTP',           'EN', 'Your Mana Community password reset OTP is {{otp}}. Valid for {{expiryMinutes}} minutes. Do not share.', '["otp","expiryMinutes"]', 'ACTIVE')
) AS t(code, name, msg_type, lang, body, vars, stat)
WHERE NOT EXISTS (SELECT 1 FROM sms_template st WHERE st.template_code = t.code AND st.language = t.lang);
