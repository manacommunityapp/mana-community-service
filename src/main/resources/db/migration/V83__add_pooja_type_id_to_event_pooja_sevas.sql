-- Migration V83: Add pooja_type_id column to event_pooja_sevas table and establish foreign key to event_pooja_types

ALTER TABLE event_pooja_sevas 
ADD COLUMN IF NOT EXISTS pooja_type_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_pooja_sevas_pooja_type_id ON event_pooja_sevas(pooja_type_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_pooja_sevas_pooja_type'
    ) THEN
        ALTER TABLE event_pooja_sevas
        ADD CONSTRAINT fk_pooja_sevas_pooja_type
        FOREIGN KEY (pooja_type_id) REFERENCES event_pooja_types(id)
        ON DELETE SET NULL;
    END IF;
END $$;
