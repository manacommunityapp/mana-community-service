-- Phase 2 flight booking integration: add flight-specific columns to community_trip.
-- Columns are nullable so existing road/bus trips are unaffected.

ALTER TABLE manacommunity.community_trip
    ADD COLUMN IF NOT EXISTS flight_origin_iata VARCHAR(3)    DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS flight_dest_iata   VARCHAR(3)    DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS group_pnr          VARCHAR(20)   DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS fare_per_person     NUMERIC(10,2) DEFAULT NULL;

COMMENT ON COLUMN manacommunity.community_trip.flight_origin_iata IS 'IATA departure airport code (FLIGHT / GROUP_FLIGHT trips)';
COMMENT ON COLUMN manacommunity.community_trip.flight_dest_iata   IS 'IATA arrival airport code (FLIGHT / GROUP_FLIGHT trips)';
COMMENT ON COLUMN manacommunity.community_trip.group_pnr          IS 'Airline group PNR locked by organizer via group desk (GROUP_FLIGHT only)';
COMMENT ON COLUMN manacommunity.community_trip.fare_per_person     IS 'Locked flight fare per seat at time of group PNR creation (GROUP_FLIGHT only)';
