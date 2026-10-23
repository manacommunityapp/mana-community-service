-- Chat message attachments: each row links a chat_message to an uploaded file.
CREATE TABLE IF NOT EXISTS manacommunity.chat_attachment (
    id              BIGSERIAL       PRIMARY KEY,
    message_id      BIGINT          NOT NULL REFERENCES manacommunity.chat_message(id) ON DELETE CASCADE,
    file_url        TEXT            NOT NULL,
    file_name       TEXT            NOT NULL,
    content_type    VARCHAR(100)    NOT NULL DEFAULT 'application/octet-stream',
    size_bytes      BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_attachment_message
    ON manacommunity.chat_attachment(message_id);
