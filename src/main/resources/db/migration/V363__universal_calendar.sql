-- Universal Calendar: personal events, reminders, family calendar, export tokens

CREATE TABLE personal_calendar_event (
    id              BIGSERIAL    PRIMARY KEY,
    community_id    BIGINT       NOT NULL,
    creator_user_id BIGINT       NOT NULL,
    calendar_type   VARCHAR(40)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    location        VARCHAR(500),
    start_time      TIMESTAMP    NOT NULL,
    end_time        TIMESTAMP    NOT NULL,
    all_day         BOOLEAN      NOT NULL DEFAULT FALSE,
    timezone        VARCHAR(60)  NOT NULL DEFAULT 'UTC',
    rrule           VARCHAR(500),
    rrule_until     TIMESTAMP,
    is_cancelled    BOOLEAN      NOT NULL DEFAULT FALSE,
    color           VARCHAR(10),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar_reminder (
    id                    BIGSERIAL    PRIMARY KEY,
    user_id               BIGINT       NOT NULL,
    event_source          VARCHAR(60)  NOT NULL,
    source_id             VARCHAR(100) NOT NULL,
    remind_minutes_before INT          NOT NULL DEFAULT 30,
    remind_at             TIMESTAMP    NOT NULL,
    sent                  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, event_source, source_id, remind_minutes_before)
);

CREATE TABLE family_calendar_member (
    id          BIGSERIAL    PRIMARY KEY,
    family_id   VARCHAR(100) NOT NULL,
    user_id     BIGINT       NOT NULL,
    invited_by  BIGINT,
    joined_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(family_id, user_id)
);

CREATE TABLE calendar_export_token (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE,
    token      VARCHAR(64) NOT NULL UNIQUE,
    calendars  TEXT        NOT NULL DEFAULT 'ALL',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pce_user   ON personal_calendar_event(creator_user_id, start_time);
CREATE INDEX idx_pce_comm   ON personal_calendar_event(community_id, start_time);
CREATE INDEX idx_cr_user    ON calendar_reminder(user_id, remind_at) WHERE NOT sent;
CREATE INDEX idx_cr_pending ON calendar_reminder(remind_at) WHERE NOT sent;
CREATE INDEX idx_fcm_user   ON family_calendar_member(user_id);
CREATE INDEX idx_fcm_family ON family_calendar_member(family_id);
