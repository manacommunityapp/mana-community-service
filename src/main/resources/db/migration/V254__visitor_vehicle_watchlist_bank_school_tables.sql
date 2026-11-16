-- Missing tables for: RegisteredChild, VehicleRegistration, GroupInvite, GroupGuest,
--                     WhitelistEntry, WatchlistEntry, BankMandate

CREATE TABLE school_bus_child (
    id           BIGSERIAL    PRIMARY KEY,
    parent_id    BIGINT       NOT NULL,
    community_id BIGINT,
    bus_id       BIGINT,
    name         VARCHAR(100) NOT NULL,
    grade        VARCHAR(20),
    school_name  VARCHAR(200),
    stop_name    VARCHAR(100),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT
);
CREATE INDEX idx_school_bus_child_parent    ON school_bus_child(parent_id);
CREATE INDEX idx_school_bus_child_community ON school_bus_child(community_id);
CREATE INDEX idx_school_bus_child_bus       ON school_bus_child(bus_id);

CREATE TABLE vehicle_registration (
    id                  BIGSERIAL    PRIMARY KEY,
    owner_id            BIGINT       NOT NULL,
    community_id        BIGINT       NOT NULL,
    plate_number        VARCHAR(20)  NOT NULL,
    vehicle_type        VARCHAR(30)  NOT NULL DEFAULT 'CAR',
    make                VARCHAR(80),
    model               VARCHAR(80),
    color               VARCHAR(40),
    is_gate_whitelisted BOOLEAN      NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);
CREATE INDEX idx_vehicle_registration_owner     ON vehicle_registration(owner_id);
CREATE INDEX idx_vehicle_registration_community ON vehicle_registration(community_id);
CREATE INDEX idx_vehicle_registration_plate     ON vehicle_registration(plate_number);

CREATE TABLE visitor_group_invite (
    id          BIGSERIAL    PRIMARY KEY,
    host_id     BIGINT       NOT NULL,
    community_id BIGINT,
    title       VARCHAR(200) NOT NULL,
    event_date  DATE,
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    pass_code   VARCHAR(12)  UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);
CREATE INDEX idx_visitor_group_invite_host      ON visitor_group_invite(host_id);
CREATE INDEX idx_visitor_group_invite_community ON visitor_group_invite(community_id);

CREATE TABLE visitor_group_guest (
    id               BIGSERIAL    PRIMARY KEY,
    group_invite_id  BIGINT       NOT NULL REFERENCES visitor_group_invite(id) ON DELETE CASCADE,
    name             VARCHAR(100) NOT NULL,
    phone            VARCHAR(15),
    vehicle_number   VARCHAR(20),
    is_checked_in    BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_visitor_group_guest_invite ON visitor_group_guest(group_invite_id);

CREATE TABLE visitor_whitelist (
    id             BIGSERIAL    PRIMARY KEY,
    resident_id    BIGINT       NOT NULL,
    community_id   BIGINT,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(15),
    category       VARCHAR(30)  NOT NULL DEFAULT 'GUEST',
    vehicle_number VARCHAR(20),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    notes          TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT
);
CREATE INDEX idx_visitor_whitelist_resident  ON visitor_whitelist(resident_id);
CREATE INDEX idx_visitor_whitelist_community ON visitor_whitelist(community_id);

CREATE TABLE watchlist_entries (
    id               BIGSERIAL    PRIMARY KEY,
    community_id     BIGINT,
    added_by_user_id BIGINT,
    name             VARCHAR(200),
    phone            VARCHAR(20),
    vehicle_number   VARCHAR(20),
    photo            VARCHAR(500),
    reason           TEXT,
    category         VARCHAR(50),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    notes            TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_by       BIGINT
);
CREATE INDEX idx_watchlist_entries_community ON watchlist_entries(community_id);
CREATE INDEX idx_watchlist_entries_status    ON watchlist_entries(status);

CREATE TABLE bank_mandates (
    id                   BIGSERIAL     PRIMARY KEY,
    user_id              BIGINT,
    community_id         BIGINT,
    bank_name            VARCHAR(100),
    account_number       VARCHAR(40),
    ifsc                 VARCHAR(20),
    account_holder_name  VARCHAR(200),
    monthly_amount       NUMERIC(14,2),
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    mandate_ref          VARCHAR(100),
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by           BIGINT,
    updated_by           BIGINT
);
CREATE INDEX idx_bank_mandates_user      ON bank_mandates(user_id);
CREATE INDEX idx_bank_mandates_community ON bank_mandates(community_id);
