-- Migration V164: Centralized Mana Community Calendar
CREATE TABLE IF NOT EXISTS community_calendar_events (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    is_all_day BOOLEAN DEFAULT FALSE,
    priority VARCHAR(30) DEFAULT 'NORMAL',
    badge VARCHAR(50),
    target_route VARCHAR(255),
    action_label VARCHAR(50),
    organizer_name VARCHAR(100),
    is_community_wide BOOLEAN DEFAULT TRUE,
    target_tower VARCHAR(50),
    community_id BIGINT,
    created_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_calendar_domain_start ON community_calendar_events(domain, start_time);
CREATE INDEX IF NOT EXISTS idx_calendar_community_start ON community_calendar_events(community_id, start_time);
