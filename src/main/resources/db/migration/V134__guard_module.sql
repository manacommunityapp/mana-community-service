-- Guard module: security guard profiles and shift management

CREATE TABLE guard_profile (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL REFERENCES community(id),
    user_id         BIGINT REFERENCES app_user(id),
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    employee_id     VARCHAR(50),
    assigned_gate   VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_guard_profile_community ON guard_profile(community_id);
CREATE INDEX idx_guard_profile_status    ON guard_profile(community_id, status);
CREATE INDEX idx_guard_profile_user      ON guard_profile(user_id);

CREATE TABLE guard_shift (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL REFERENCES community(id),
    guard_id        BIGINT NOT NULL REFERENCES guard_profile(id) ON DELETE CASCADE,
    shift_date      DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    gate            VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    check_in_time   TIMESTAMP,
    check_out_time  TIMESTAMP,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_guard_shift_community ON guard_shift(community_id);
CREATE INDEX idx_guard_shift_guard     ON guard_shift(guard_id);
CREATE INDEX idx_guard_shift_date      ON guard_shift(community_id, shift_date);
