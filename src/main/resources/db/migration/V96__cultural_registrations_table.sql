-- Dedicated registration table for cultural sub-events.
-- Replaces the generic event_booking_registrations path for cultural-prefixed activity IDs.

CREATE TABLE event_cultural_registrations (
    id                   BIGSERIAL PRIMARY KEY,
    reg_code             VARCHAR(100) NOT NULL UNIQUE,
    cultural_event_id    BIGINT       NOT NULL,
    main_event_id        BIGINT,
    community_id         BIGINT,
    user_id              BIGINT,
    participant_name     VARCHAR(255) NOT NULL,
    gotram               VARCHAR(100),
    devotee_count        INT          NOT NULL DEFAULT 1,
    members_json         TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    registration_source  VARCHAR(20)  NOT NULL DEFAULT 'SELF',
    registered_by        BIGINT,
    override_used        BOOLEAN      NOT NULL DEFAULT FALSE,
    qr_code_url          VARCHAR(500),
    checked_in           BOOLEAN      NOT NULL DEFAULT FALSE,
    checked_in_at        TIMESTAMP,
    cancellation_reason  VARCHAR(500),
    cancelled_at         TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP,
    created_by           BIGINT,
    updated_by           BIGINT
);

CREATE INDEX idx_ecr_cultural_event_id ON event_cultural_registrations (cultural_event_id);
CREATE INDEX idx_ecr_user_id           ON event_cultural_registrations (user_id);
CREATE INDEX idx_ecr_main_event_id     ON event_cultural_registrations (main_event_id);
CREATE INDEX idx_ecr_status            ON event_cultural_registrations (status);
CREATE INDEX idx_ecr_community_id      ON event_cultural_registrations (community_id);
