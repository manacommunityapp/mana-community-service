CREATE TYPE slot_status AS ENUM ('OPEN', 'FULL', 'CUTOFF_PASSED', 'CANCELLED');

CREATE TABLE delivery_slot_template (
    id              BIGSERIAL PRIMARY KEY,
    vendor_id       BIGINT NOT NULL,
    community_id    BIGINT NOT NULL,
    day_of_week     INT NOT NULL,
    slot_start      TIME NOT NULL,
    slot_end        TIME NOT NULL,
    capacity        INT NOT NULL DEFAULT 50,
    cutoff_hours    INT NOT NULL DEFAULT 2,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE delivery_slot (
    id              BIGSERIAL PRIMARY KEY,
    template_id     BIGINT REFERENCES delivery_slot_template(id),
    vendor_id       BIGINT NOT NULL,
    community_id    BIGINT NOT NULL,
    slot_date       DATE NOT NULL,
    slot_start      TIMESTAMP NOT NULL,
    slot_end        TIMESTAMP NOT NULL,
    cutoff_at       TIMESTAMP NOT NULL,
    capacity        INT NOT NULL,
    booked_count    INT NOT NULL DEFAULT 0,
    status          slot_status NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(vendor_id, community_id, slot_date, slot_start)
);

CREATE INDEX idx_ds_vendor_date ON delivery_slot(vendor_id, community_id, slot_date, status);
