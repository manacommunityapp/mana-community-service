-- V138__parking_management.sql
-- Parking management tables and initial seed data

CREATE TABLE IF NOT EXISTS manacommunity.parking_spot (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    spot_number VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    spot_type VARCHAR(20) NOT NULL DEFAULT 'CAR',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    vehicle_number VARCHAR(50),
    owner_name VARCHAR(100),
    owner_flat VARCHAR(50),
    assigned_user_id BIGINT,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT fk_parking_spot_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_parking_spot_user FOREIGN KEY (assigned_user_id) REFERENCES manacommunity.app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_parking_spot_community ON manacommunity.parking_spot(community_id);
CREATE INDEX IF NOT EXISTS idx_parking_spot_user ON manacommunity.parking_spot(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_parking_spot_status ON manacommunity.parking_spot(status);
CREATE INDEX IF NOT EXISTS idx_parking_spot_type ON manacommunity.parking_spot(spot_type);

CREATE TABLE IF NOT EXISTS manacommunity.parking_visitor_pass (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    pass_code VARCHAR(50) NOT NULL UNIQUE,
    visitor_name VARCHAR(100) NOT NULL,
    visitor_phone VARCHAR(20),
    vehicle_number VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'CAR',
    spot_id BIGINT,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT fk_parking_visitor_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_parking_visitor_user FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_parking_visitor_spot FOREIGN KEY (spot_id) REFERENCES manacommunity.parking_spot(id)
);

CREATE INDEX IF NOT EXISTS idx_parking_visitor_community ON manacommunity.parking_visitor_pass(community_id);
CREATE INDEX IF NOT EXISTS idx_parking_visitor_user ON manacommunity.parking_visitor_pass(user_id);
CREATE INDEX IF NOT EXISTS idx_parking_visitor_pass_code ON manacommunity.parking_visitor_pass(pass_code);
CREATE INDEX IF NOT EXISTS idx_parking_visitor_status ON manacommunity.parking_visitor_pass(status);
