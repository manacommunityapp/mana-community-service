-- ============================================================================
-- V274: Mana Kitty Party Events, RSVP Calendar & Attendance
-- Schema: manacommunity
-- ============================================================================

-- 1. Kitty Party Event Master Table
CREATE TABLE IF NOT EXISTS manacommunity.kitty_event (
    id                 BIGSERIAL PRIMARY KEY,
    circle_id          BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    event_name         VARCHAR(200) NOT NULL,
    description        TEXT,
    event_type         VARCHAR(50) NOT NULL DEFAULT 'KITTY_MONTHLY'
                       CHECK (event_type IN ('KITTY_MONTHLY', 'THEMED_PARTY', 'CASUAL_MEETUP', 'FESTIVAL_CELEBRATION', 'OTHER')),
    start_time         TIMESTAMP NOT NULL,
    end_time           TIMESTAMP NOT NULL,
    timezone           VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    venue_type         VARCHAR(30) NOT NULL DEFAULT 'HOST_HOME'
                       CHECK (venue_type IN ('HOST_HOME', 'COMMUNITY_HALL', 'RESTAURANT', 'ONLINE_VIRTUAL', 'OTHER')),
    venue_address      TEXT,
    host_user_id       BIGINT REFERENCES manacommunity.app_user(id),
    max_attendees      INTEGER CHECK (max_attendees IS NULL OR max_attendees > 0),
    recurrence_rule    VARCHAR(30) DEFAULT 'NONE'
                       CHECK (recurrence_rule IN ('NONE', 'MONTHLY', 'BI_WEEKLY')),
    status             VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED'
                       CHECK (status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_by         BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_kitty_event_time CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_kitty_event_circle ON manacommunity.kitty_event(circle_id, status);
CREATE INDEX IF NOT EXISTS idx_kitty_event_start ON manacommunity.kitty_event(start_time);
CREATE INDEX IF NOT EXISTS idx_kitty_event_host ON manacommunity.kitty_event(host_user_id);

-- 2. Kitty Party RSVP Records
CREATE TABLE IF NOT EXISTS manacommunity.kitty_event_rsvp (
    id                 BIGSERIAL PRIMARY KEY,
    event_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_event(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    response           VARCHAR(20) NOT NULL
                       CHECK (response IN ('GOING', 'NOT_GOING', 'MAYBE', 'WAITLISTED')),
    guest_count        INTEGER NOT NULL DEFAULT 0 CHECK (guest_count >= 0),
    notes              TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_event_rsvp UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_kitty_rsvp_event ON manacommunity.kitty_event_rsvp(event_id, response);
CREATE INDEX IF NOT EXISTS idx_kitty_rsvp_user ON manacommunity.kitty_event_rsvp(user_id);

-- 3. Kitty Party Event Attendance Check-in
CREATE TABLE IF NOT EXISTS manacommunity.kitty_event_attendance (
    id                 BIGSERIAL PRIMARY KEY,
    event_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_event(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    attendance_status  VARCHAR(20) NOT NULL DEFAULT 'PRESENT'
                       CHECK (attendance_status IN ('PRESENT', 'ABSENT', 'LATE')),
    marked_by          BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    marked_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitty_event_attendance UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_kitty_att_event ON manacommunity.kitty_event_attendance(event_id, attendance_status);
