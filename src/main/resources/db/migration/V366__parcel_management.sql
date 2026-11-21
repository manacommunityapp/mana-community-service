CREATE TYPE parcel_status AS ENUM ('RECEIVED','NOTIFIED','PICKED_UP','RETURNED','LOST');

CREATE TABLE parcel_log (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT NOT NULL,
    flat_number      VARCHAR(20) NOT NULL,
    resident_user_id BIGINT,
    sender_name      VARCHAR(255),
    courier_name     VARCHAR(100),
    tracking_number  VARCHAR(100),
    description      VARCHAR(500),
    status           parcel_status NOT NULL DEFAULT 'RECEIVED',
    received_by      BIGINT NOT NULL,
    received_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    notified_at      TIMESTAMP,
    picked_up_by     BIGINT,
    picked_up_at     TIMESTAMP,
    pickup_otp       VARCHAR(6),
    image_key        VARCHAR(500),
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parcel_comm ON parcel_log(community_id, status);
CREATE INDEX idx_parcel_flat ON parcel_log(community_id, flat_number, status);
CREATE INDEX idx_parcel_res  ON parcel_log(resident_user_id, status);
