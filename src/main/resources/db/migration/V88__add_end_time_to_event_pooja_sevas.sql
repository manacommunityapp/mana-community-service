-- Migration V88: Add end_time column to event_pooja_sevas

ALTER TABLE manacommunity.event_pooja_sevas 
    ADD COLUMN IF NOT EXISTS end_time TIME;
