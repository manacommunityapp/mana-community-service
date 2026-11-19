-- Home Services: workers, availability, booking, packages
CREATE TYPE hs_service_category AS ENUM (
    'PLUMBING','ELECTRICAL','CARPENTRY','PAINTING','CLEANING','PEST_CONTROL',
    'AC_SERVICE','APPLIANCE_REPAIR','GARDENING','LAUNDRY','SECURITY','OTHER'
);

CREATE TYPE hs_booking_type AS ENUM ('INSTANT','SCHEDULED');
CREATE TYPE hs_booking_status AS ENUM (
    'PENDING','ASSIGNED','WORKER_EN_ROUTE','IN_PROGRESS',
    'WORK_DONE','COMPLETED','CANCELLED','NO_SHOW'
);

CREATE TABLE hs_worker (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(255),
    is_external     BOOLEAN NOT NULL DEFAULT FALSE,
    rating          NUMERIC(3,2),
    total_jobs      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE hs_worker_skill (
    id              BIGSERIAL PRIMARY KEY,
    worker_id       BIGINT NOT NULL REFERENCES hs_worker(id) ON DELETE CASCADE,
    category        hs_service_category NOT NULL,
    years_exp       INT,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(worker_id, category)
);

CREATE TABLE hs_worker_availability (
    id              BIGSERIAL PRIMARY KEY,
    worker_id       BIGINT NOT NULL REFERENCES hs_worker(id) ON DELETE CASCADE,
    community_id    BIGINT NOT NULL,
    available_date  DATE NOT NULL,
    slot_start      TIME NOT NULL,
    slot_end        TIME NOT NULL,
    is_booked       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(worker_id, available_date, slot_start)
);

CREATE TABLE hs_service_package (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    category        hs_service_category NOT NULL,
    description     TEXT,
    duration_minutes INT NOT NULL DEFAULT 60,
    base_price      NUMERIC(12,2) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE hs_booking (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    worker_id       BIGINT REFERENCES hs_worker(id),
    package_id      BIGINT REFERENCES hs_service_package(id),
    category        hs_service_category NOT NULL,
    booking_type    hs_booking_type NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at    TIMESTAMP,
    status          hs_booking_status NOT NULL DEFAULT 'PENDING',
    flat_number     VARCHAR(30),
    description     TEXT,
    quoted_price    NUMERIC(12,2),
    final_price     NUMERIC(12,2),
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hsb_community   ON hs_booking(community_id, status);
CREATE INDEX idx_hsb_user        ON hs_booking(user_id, status);
CREATE INDEX idx_hsb_worker      ON hs_booking(worker_id, status);
CREATE INDEX idx_hswa_worker     ON hs_worker_availability(worker_id, available_date, is_booked);
