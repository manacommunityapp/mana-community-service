-- Migration V55: Add end_date and multi_day columns to event_pooja_sevas table

ALTER TABLE event_pooja_sevas ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE event_pooja_sevas ADD COLUMN IF NOT EXISTS multi_day BOOLEAN DEFAULT FALSE;
