-- Migration V542: Geospatial Geofence Boundaries, Multi-Gate Topology, and Canonical Society Address Registry

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS manacommunity.community_geofences (
    id                   BIGSERIAL PRIMARY KEY,
    community_id         BIGINT NOT NULL UNIQUE,
    name                 VARCHAR(150) NOT NULL,
    polygon_vertices     JSONB NOT NULL,
    min_lat              DOUBLE PRECISION NOT NULL,
    max_lat              DOUBLE PRECISION NOT NULL,
    min_lon              DOUBLE PRECISION NOT NULL,
    max_lon              DOUBLE PRECISION NOT NULL,
    center_lat           DOUBLE PRECISION NOT NULL,
    center_lon           DOUBLE PRECISION NOT NULL,
    area_sq_meters       DOUBLE PRECISION,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_community_geofences_aabb 
ON manacommunity.community_geofences (min_lat, max_lat, min_lon, max_lon);

CREATE TABLE IF NOT EXISTS manacommunity.community_gates (
    id                   BIGSERIAL PRIMARY KEY,
    community_id         BIGINT NOT NULL,
    gate_code            VARCHAR(20) NOT NULL,
    gate_name            VARCHAR(100) NOT NULL,
    gate_type            VARCHAR(30) NOT NULL DEFAULT 'ALL_TRAFFIC',
    latitude             DOUBLE PRECISION NOT NULL,
    longitude            DOUBLE PRECISION NOT NULL,
    intercom_extension   VARCHAR(20),
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_community_gate_code UNIQUE (community_id, gate_code)
);

CREATE INDEX IF NOT EXISTS idx_community_gates_comm_type 
ON manacommunity.community_gates (community_id, gate_type, is_active);

CREATE TABLE IF NOT EXISTS manacommunity.society_canonical_registry (
    id                   BIGSERIAL PRIMARY KEY,
    community_id         BIGINT NOT NULL,
    canonical_name       VARCHAR(150) NOT NULL,
    alias_names          TEXT[],
    locality             VARCHAR(100) NOT NULL,
    city                 VARCHAR(100) NOT NULL,
    pincode              VARCHAR(10) NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_society_registry_trgm 
ON manacommunity.society_canonical_registry USING GIN (canonical_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_society_registry_pin_city 
ON manacommunity.society_canonical_registry (pincode, city);
