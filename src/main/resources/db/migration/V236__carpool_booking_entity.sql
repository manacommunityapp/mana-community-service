CREATE TABLE IF NOT EXISTS mobility_carpool_booking (
    id         BIGSERIAL PRIMARY KEY,
    offer_id   BIGINT      NOT NULL REFERENCES mobility_carpool_offer(id),
    user_id    BIGINT      NOT NULL REFERENCES app_user(id),
    seats      SMALLINT    NOT NULL DEFAULT 1,
    status     VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    booked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_mobility_carpool_booking_offer_user UNIQUE (offer_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_mobility_carpool_booking_user
    ON mobility_carpool_booking (user_id, booked_at DESC);
