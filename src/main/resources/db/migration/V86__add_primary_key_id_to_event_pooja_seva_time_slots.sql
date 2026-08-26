-- Migration V86: Add primary key id column to event_pooja_seva_time_slots table

CREATE TABLE IF NOT EXISTS event_pooja_seva_time_slots (
    id BIGSERIAL PRIMARY KEY,
    pooja_seva_id BIGINT NOT NULL,
    slot_date DATE,
    start_time VARCHAR(20),
    end_time VARCHAR(20),
    title VARCHAR(200),
    slot_count INT,
    CONSTRAINT fk_pooja_seva_time_slots FOREIGN KEY (pooja_seva_id) REFERENCES event_pooja_sevas(id) ON DELETE CASCADE
);

DO $$
BEGIN
    -- 1. Check if column id exists, if not add it as BIGSERIAL
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'event_pooja_seva_time_slots' AND column_name = 'id'
    ) THEN
        ALTER TABLE event_pooja_seva_time_slots ADD COLUMN id BIGSERIAL;
    END IF;

    -- 2. Check if primary key exists on event_pooja_seva_time_slots, if not add it
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'event_pooja_seva_time_slots' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE event_pooja_seva_time_slots ADD PRIMARY KEY (id);
    END IF;
END $$;
