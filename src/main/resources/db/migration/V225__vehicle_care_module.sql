-- Vehicle Care Module (Washcaro integration)
-- Resident vehicle profiles, service catalog, slot bookings, and recurring wash plans

CREATE TABLE vehicle_profile (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT       NOT NULL REFERENCES app_user(id),
    community_id    BIGINT       NOT NULL,
    nickname        VARCHAR(80)  NOT NULL,
    vehicle_type    VARCHAR(20)  NOT NULL,
    make            VARCHAR(50)  NOT NULL,
    model           VARCHAR(80)  NOT NULL,
    color           VARCHAR(30),
    plate_number    VARCHAR(20)  NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vehicle_profile_owner     ON vehicle_profile(owner_id);
CREATE INDEX idx_vehicle_profile_community ON vehicle_profile(community_id);

CREATE TABLE vehicle_service_catalog (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    description      TEXT,
    vehicle_types    VARCHAR(200),
    duration_minutes INT            NOT NULL,
    base_price       NUMERIC(10,2)  NOT NULL,
    community_price  NUMERIC(10,2)  NOT NULL,
    is_popular       BOOLEAN        NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    gradient_start   VARCHAR(20),
    gradient_end     VARCHAR(20),
    sort_order       INT            NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE vehicle_service_addon (
    id         BIGSERIAL PRIMARY KEY,
    service_id BIGINT         NOT NULL REFERENCES vehicle_service_catalog(id),
    name       VARCHAR(100)   NOT NULL,
    price      NUMERIC(10,2)  NOT NULL,
    is_active  BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vc_addon_service ON vehicle_service_addon(service_id);

CREATE TABLE vehicle_service_slot (
    id              BIGSERIAL PRIMARY KEY,
    slot_date       DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    service_id      BIGINT       REFERENCES vehicle_service_catalog(id),
    total_capacity  INT          NOT NULL,
    booked_count    INT          NOT NULL DEFAULT 0,
    community_id    BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vc_slot_date_service ON vehicle_service_slot(slot_date, service_id);

CREATE TABLE vehicle_care_booking (
    id               BIGSERIAL PRIMARY KEY,
    booking_number   VARCHAR(30)    NOT NULL UNIQUE,
    resident_id      BIGINT         NOT NULL REFERENCES app_user(id),
    community_id     BIGINT         NOT NULL,
    vehicle_id       BIGINT         NOT NULL REFERENCES vehicle_profile(id),
    service_id       BIGINT         NOT NULL REFERENCES vehicle_service_catalog(id),
    slot_id          BIGINT         NOT NULL REFERENCES vehicle_service_slot(id),
    technician_id    BIGINT         REFERENCES app_user(id),
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING_PAYMENT',
    base_amount      NUMERIC(10,2)  NOT NULL,
    addon_amount     NUMERIC(10,2)  NOT NULL DEFAULT 0,
    total_amount     NUMERIC(10,2)  NOT NULL,
    payment_order_id VARCHAR(100),
    payment_reference VARCHAR(100),
    location_notes   VARCHAR(300),
    technician_notes TEXT,
    rating           INT,
    review_text      TEXT,
    eta_minutes      INT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vc_booking_resident  ON vehicle_care_booking(resident_id);
CREATE INDEX idx_vc_booking_community ON vehicle_care_booking(community_id);
CREATE INDEX idx_vc_booking_slot      ON vehicle_care_booking(slot_id);
CREATE INDEX idx_vc_booking_status    ON vehicle_care_booking(status);

CREATE TABLE vehicle_booking_addon (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT        NOT NULL REFERENCES vehicle_care_booking(id) ON DELETE CASCADE,
    addon_id   BIGINT        NOT NULL REFERENCES vehicle_service_addon(id),
    price      NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vc_booking_addon_booking ON vehicle_booking_addon(booking_id);

CREATE TABLE vehicle_care_plan_catalog (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    description      TEXT,
    frequency_label  VARCHAR(50)    NOT NULL,
    washes_per_month INT            NOT NULL,
    price            NUMERIC(10,2)  NOT NULL,
    original_price   NUMERIC(10,2)  NOT NULL,
    vehicle_types    VARCHAR(200),
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order       INT            NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE vehicle_care_plan (
    id              BIGSERIAL PRIMARY KEY,
    resident_id     BIGINT        NOT NULL REFERENCES app_user(id),
    community_id    BIGINT        NOT NULL,
    plan_catalog_id BIGINT        NOT NULL REFERENCES vehicle_care_plan_catalog(id),
    vehicle_id      BIGINT        NOT NULL REFERENCES vehicle_profile(id),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    credits_total   INT           NOT NULL,
    credits_used    INT           NOT NULL DEFAULT 0,
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,
    payment_reference VARCHAR(100),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vc_plan_resident  ON vehicle_care_plan(resident_id);
CREATE INDEX idx_vc_plan_community ON vehicle_care_plan(community_id);

-- Seed catalog: 6 services
INSERT INTO vehicle_service_catalog
    (name, description, vehicle_types, duration_minutes, base_price, community_price, is_popular, gradient_start, gradient_end, sort_order)
VALUES
    ('Express Exterior Wash',  'Quick foam wash + rinse',                 'CAR,BIKE,SUV', 30,  299, 249, TRUE,  '#0284C7', '#0EA5E9', 1),
    ('Interior Vacuum',        'Deep vacuum seats, mats & dashboard',      'CAR,SUV',      45,  349, 299, FALSE, '#7C3AED', '#8B5CF6', 2),
    ('Full Detail Wash',       'Exterior + interior + tyre shine',         'CAR,SUV',      90,  699, 599, TRUE,  '#0F766E', '#0D9488', 3),
    ('Ceramic Coating',        'Long-lasting nano-ceramic protective coat', 'CAR,SUV',     240, 3999,3499,FALSE, '#D97706', '#F59E0B', 4),
    ('Bike Full Service Wash', 'Frame, chain, wheels & seat',              'BIKE',          45,  249, 199, TRUE,  '#DC2626', '#EF4444', 5),
    ('Engine Bay Cleaning',    'Degrease & clean engine bay',              'CAR,SUV',       60,  799, 699, FALSE, '#374151', '#6B7280', 6);

-- Seed addons
INSERT INTO vehicle_service_addon (service_id, name, price)
SELECT id, 'Tyre Dressing', 79  FROM vehicle_service_catalog WHERE name = 'Express Exterior Wash'
UNION ALL
SELECT id, 'Dashboard Polish', 99 FROM vehicle_service_catalog WHERE name = 'Interior Vacuum'
UNION ALL
SELECT id, 'Fragrance Treatment', 49 FROM vehicle_service_catalog WHERE name = 'Full Detail Wash'
UNION ALL
SELECT id, 'Windshield Treatment', 149 FROM vehicle_service_catalog WHERE name = 'Full Detail Wash';

-- Seed plan catalog
INSERT INTO vehicle_care_plan_catalog
    (name, frequency_label, washes_per_month, price, original_price, vehicle_types, sort_order)
VALUES
    ('Basic Plan',    'Every 4 Days',  8,  999, 1400, 'CAR,BIKE,SUV', 1),
    ('Standard Plan', 'Every 3 Days', 10, 1299, 1800, 'CAR,SUV',      2),
    ('Premium Plan',  'Every 2 Days', 15, 1799, 2500, 'CAR,SUV',      3),
    ('Daily Shine',   'Daily',        30, 2999, 4200, 'CAR,SUV',      4);
