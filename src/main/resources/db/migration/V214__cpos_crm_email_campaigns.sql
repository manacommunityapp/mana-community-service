-- Bulk email campaigns targeting filtered lead segments
CREATE TABLE cpos_crm_email_campaign (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body_html       TEXT         NOT NULL,
    filter_stage    VARCHAR(50)  NULL,
    filter_source   VARCHAR(50)  NULL,
    filter_tag_id   BIGINT       NULL,
    sent_count      INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL,
    sent_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_email_campaign_community FOREIGN KEY (community_id) REFERENCES community (id)
);
