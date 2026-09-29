-- Parking slots belonging to a community
CREATE TABLE IF NOT EXISTS manacommunity.parking_slot (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    slot_number VARCHAR(20) NOT NULL,
    zone VARCHAR(40),
    floor VARCHAR(20),
    slot_type VARCHAR(20) NOT NULL DEFAULT 'COVERED',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    assigned_to_id BIGINT,
    vehicle_id BIGINT,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_parking_slot_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_parking_slot_assigned FOREIGN KEY (assigned_to_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT uk_parking_slot_community_number UNIQUE (community_id, slot_number)
);
CREATE INDEX IF NOT EXISTS idx_parking_slot_community ON manacommunity.parking_slot(community_id);
CREATE INDEX IF NOT EXISTS idx_parking_slot_status ON manacommunity.parking_slot(status);
CREATE INDEX IF NOT EXISTS idx_parking_slot_assigned ON manacommunity.parking_slot(assigned_to_id);

-- Resident vehicles registered in a community
CREATE TABLE IF NOT EXISTS manacommunity.resident_vehicle (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL DEFAULT 'CAR',
    make VARCHAR(60),
    model VARCHAR(60),
    color VARCHAR(30),
    number_plate VARCHAR(20) NOT NULL,
    parking_slot_id BIGINT,
    sticker_number VARCHAR(30),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_resident_vehicle_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_resident_vehicle_owner FOREIGN KEY (owner_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_resident_vehicle_slot FOREIGN KEY (parking_slot_id) REFERENCES manacommunity.parking_slot(id),
    CONSTRAINT uk_resident_vehicle_community_plate UNIQUE (community_id, number_plate)
);
CREATE INDEX IF NOT EXISTS idx_resident_vehicle_community ON manacommunity.resident_vehicle(community_id);
CREATE INDEX IF NOT EXISTS idx_resident_vehicle_owner ON manacommunity.resident_vehicle(owner_id);
CREATE INDEX IF NOT EXISTS idx_resident_vehicle_slot ON manacommunity.resident_vehicle(parking_slot_id);
