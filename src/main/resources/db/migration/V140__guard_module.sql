-- Guard module: security guard profiles and shift management

CREATE TABLE IF NOT EXISTS manacommunity.guard_profile (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT,
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    employee_id     VARCHAR(50),
    assigned_gate   VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guard_profile_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_guard_profile_user FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_guard_profile_community ON manacommunity.guard_profile(community_id);
CREATE INDEX IF NOT EXISTS idx_guard_profile_status    ON manacommunity.guard_profile(community_id, status);
CREATE INDEX IF NOT EXISTS idx_guard_profile_user      ON manacommunity.guard_profile(user_id);

CREATE TABLE IF NOT EXISTS manacommunity.guard_shift (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    guard_id        BIGINT NOT NULL,
    shift_date      DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    gate            VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    check_in_time   TIMESTAMP,
    check_out_time  TIMESTAMP,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guard_shift_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_guard_shift_guard FOREIGN KEY (guard_id) REFERENCES manacommunity.guard_profile(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_guard_shift_community ON manacommunity.guard_shift(community_id);
CREATE INDEX IF NOT EXISTS idx_guard_shift_guard     ON manacommunity.guard_shift(guard_id);
CREATE INDEX IF NOT EXISTS idx_guard_shift_date      ON manacommunity.guard_shift(community_id, shift_date);
