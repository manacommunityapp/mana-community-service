-- V72: Production-grade Pooja Schedule and Slot Reservation
-- Replaces embedded PoojaSevaDayTimeSlot with a lockable, first-class entity.
-- pooja_slot_reservation enables pessimistic capacity control with expiry.

CREATE TABLE IF NOT EXISTS pooja_schedule (
    id               BIGSERIAL PRIMARY KEY,
    pooja_id         BIGINT        NOT NULL,
    schedule_date    DATE          NOT NULL,
    start_time       TIME          NOT NULL,
    end_time         TIME,
    family_capacity  INTEGER       NOT NULL DEFAULT 10,
    devotee_capacity INTEGER       NOT NULL DEFAULT 30,
    status           VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    next_token_seq   INTEGER       NOT NULL DEFAULT 1,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    CONSTRAINT fk_ps_pooja    FOREIGN KEY (pooja_id) REFERENCES event_pooja_sevas(id) ON DELETE CASCADE,
    CONSTRAINT uq_pooja_schedule_slot UNIQUE (pooja_id, schedule_date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_ps_pooja_date  ON pooja_schedule(pooja_id, schedule_date);
CREATE INDEX IF NOT EXISTS idx_ps_status      ON pooja_schedule(status);

-- ------------------------------------------------------------------
-- Temporary capacity hold — 5-minute default window (configurable).
-- status: RESERVED | PAYMENT_PENDING | CONFIRMED | EXPIRED | CANCELLED
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pooja_slot_reservation (
    id                     BIGSERIAL    PRIMARY KEY,
    schedule_id            BIGINT       NOT NULL,
    user_id                BIGINT       NOT NULL,
    registration_id        BIGINT,
    reserved_family_count  INTEGER      NOT NULL DEFAULT 1,
    reserved_devotee_count INTEGER      NOT NULL DEFAULT 1,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'RESERVED',
    expires_at             TIMESTAMP    NOT NULL,
    idempotency_key        VARCHAR(100),
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP,
    CONSTRAINT fk_psr_schedule FOREIGN KEY (schedule_id) REFERENCES pooja_schedule(id),
    CONSTRAINT uq_psr_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_psr_schedule_status ON pooja_slot_reservation(schedule_id, status, expires_at);
CREATE INDEX IF NOT EXISTS idx_psr_user            ON pooja_slot_reservation(user_id);
CREATE INDEX IF NOT EXISTS idx_psr_expires         ON pooja_slot_reservation(expires_at, status);
