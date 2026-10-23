-- Trip booking module

CREATE TABLE IF NOT EXISTS manacommunity.trips (
    id                    BIGSERIAL PRIMARY KEY,
    community_id          BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    organizer_id          BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    title                 VARCHAR(150)  NOT NULL,
    description           TEXT,
    destination           VARCHAR(200)  NOT NULL,
    trip_type             VARCHAR(20)   NOT NULL DEFAULT 'DAY_TRIP',
    start_date            DATE          NOT NULL,
    end_date              DATE,
    max_participants      INT,
    current_participants  INT           NOT NULL DEFAULT 0,
    estimated_cost        NUMERIC(12,2),
    meeting_point         VARCHAR(200),
    status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    notes                 TEXT,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trip_community  ON manacommunity.trips(community_id);
CREATE INDEX IF NOT EXISTS idx_trip_organizer  ON manacommunity.trips(organizer_id);
CREATE INDEX IF NOT EXISTS idx_trip_start_date ON manacommunity.trips(start_date);
