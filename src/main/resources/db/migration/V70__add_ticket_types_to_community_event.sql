-- Migration V70: Add ticket_types_json column to community_event table

ALTER TABLE community_event ADD COLUMN IF NOT EXISTS ticket_types_json TEXT;
