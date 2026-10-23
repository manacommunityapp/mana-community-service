-- Community Purchase Calendar: recurring procurement scheduling

CREATE TABLE purchase_calendar_entries (
    id                     BIGSERIAL      PRIMARY KEY,
    title                  VARCHAR(200)   NOT NULL,
    description            VARCHAR(1000),
    category               VARCHAR(50)    NOT NULL DEFAULT 'OTHER',
    frequency              VARCHAR(30)    NOT NULL,
    unit                   VARCHAR(50)    NOT NULL DEFAULT 'units',
    quantity               INT            NOT NULL DEFAULT 1,
    estimated_budget       DECIMAL(14,2),
    delivery_location      VARCHAR(300),
    next_due_date          DATE           NOT NULL,
    last_rfq_date          DATE,
    last_rfq_id            BIGINT,
    is_active              BOOLEAN        NOT NULL DEFAULT TRUE,
    community_id           BIGINT         NOT NULL,
    notes                  VARCHAR(500),
    preferred_supplier_ids VARCHAR(500),
    created_by             VARCHAR(120),
    created_at             TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cal_community ON purchase_calendar_entries (community_id);
CREATE INDEX idx_cal_next_due  ON purchase_calendar_entries (next_due_date);
CREATE INDEX idx_cal_active    ON purchase_calendar_entries (is_active);
