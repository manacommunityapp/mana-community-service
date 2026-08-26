-- Migration V85: Add end_time column to event_pooja_seva_time_slots table

CREATE TABLE IF NOT EXISTS event_pooja_seva_time_slots (
    pooja_seva_id BIGINT NOT NULL,
    slot_date DATE,
    start_time VARCHAR(20),
    end_time VARCHAR(20),
    title VARCHAR(200),
    slot_count INT,
    CONSTRAINT fk_pooja_seva_time_slots FOREIGN KEY (pooja_seva_id) REFERENCES event_pooja_sevas(id) ON DELETE CASCADE
);

ALTER TABLE event_pooja_seva_time_slots ADD COLUMN IF NOT EXISTS end_time VARCHAR(20);
