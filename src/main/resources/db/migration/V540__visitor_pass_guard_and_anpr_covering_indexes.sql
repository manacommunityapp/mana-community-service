-- Migration V540: Add covering indexes for high-velocity guard desk lookups and ANPR vehicle queries

CREATE INDEX IF NOT EXISTS idx_visitor_pass_guard_search 
    ON visitor_pass (community_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_visitor_pass_vehicle_anpr 
    ON visitor_pass ((UPPER(REPLACE(REPLACE(vehicle_number, ' ', ''), '-', ''))))
    WHERE vehicle_number IS NOT NULL;
