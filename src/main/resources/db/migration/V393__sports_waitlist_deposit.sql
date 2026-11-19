-- Court slot waitlist
CREATE TABLE court_slot_waitlist (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    booking_id      BIGINT,
    user_id         BIGINT NOT NULL,
    position        INTEGER,
    notified        BOOLEAN NOT NULL DEFAULT FALSE,
    notified_at     TIMESTAMP,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_csw_community ON court_slot_waitlist(community_id, user_id);

-- Sports no-show deposit
CREATE TABLE sports_no_show_deposit (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    booking_id      BIGINT NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    held_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    released        BOOLEAN NOT NULL DEFAULT FALSE,
    released_at     TIMESTAMP,
    forfeited       BOOLEAN NOT NULL DEFAULT FALSE,
    forfeited_at    TIMESTAMP,
    reason          VARCHAR(500)
);

CREATE INDEX idx_snsd_booking ON sports_no_show_deposit(booking_id);
CREATE INDEX idx_snsd_user    ON sports_no_show_deposit(user_id, forfeited);
