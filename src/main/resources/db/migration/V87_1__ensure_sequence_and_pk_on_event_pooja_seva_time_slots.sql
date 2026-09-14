-- Migration V87: Ensure sequence, default nextval, and primary key on event_pooja_seva_time_slots

-- 1. Create dedicated sequence in manacommunity schema if it does not exist
CREATE SEQUENCE IF NOT EXISTS manacommunity.event_pooja_seva_time_slots_id_seq;

-- 2. Ensure column id exists in table
ALTER TABLE manacommunity.event_pooja_seva_time_slots 
    ADD COLUMN IF NOT EXISTS id BIGINT;

-- 3. Set default to nextval of the sequence
ALTER TABLE manacommunity.event_pooja_seva_time_slots 
    ALTER COLUMN id SET DEFAULT nextval('manacommunity.event_pooja_seva_time_slots_id_seq');

-- 4. Backfill any existing NULL ids from the sequence
UPDATE manacommunity.event_pooja_seva_time_slots 
SET id = nextval('manacommunity.event_pooja_seva_time_slots_id_seq') 
WHERE id IS NULL;

-- 5. Ensure id is NOT NULL
ALTER TABLE manacommunity.event_pooja_seva_time_slots 
    ALTER COLUMN id SET NOT NULL;

-- 6. Own sequence by table column
ALTER SEQUENCE manacommunity.event_pooja_seva_time_slots_id_seq 
    OWNED BY manacommunity.event_pooja_seva_time_slots.id;

-- 7. Ensure primary key constraint exists on id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conrelid = 'manacommunity.event_pooja_seva_time_slots'::regclass 
        AND contype = 'p'
    ) THEN
        ALTER TABLE manacommunity.event_pooja_seva_time_slots ADD PRIMARY KEY (id);
    END IF;
END $$;
