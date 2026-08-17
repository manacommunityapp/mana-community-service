-- Migration V56: Add multiple start times collection table for event_pooja_sevas

CREATE TABLE IF NOT EXISTS event_pooja_seva_start_times (
    pooja_seva_id BIGINT NOT NULL,
    start_time_slot VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pooja_seva_start_times FOREIGN KEY (pooja_seva_id) REFERENCES event_pooja_sevas(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pooja_seva_start_times_id ON event_pooja_seva_start_times(pooja_seva_id);
