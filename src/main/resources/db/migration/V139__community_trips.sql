-- V139__community_trips.sql
-- Community trips and resident bookings

CREATE TABLE IF NOT EXISTS manacommunity.community_trip (
    id VARCHAR(50) PRIMARY KEY,
    community_id BIGINT,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    departure_date VARCHAR(100) NOT NULL,
    departure_point VARCHAR(200) NOT NULL,
    duration VARCHAR(100),
    total_seats INT NOT NULL DEFAULT 20,
    booked_seats INT NOT NULL DEFAULT 0,
    price_per_person DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    host_name VARCHAR(150),
    host_flat VARCHAR(50),
    transport VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    description TEXT,
    highlights TEXT,
    includes TEXT,
    excludes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manacommunity.community_trip_booking (
    id VARCHAR(50) PRIMARY KEY,
    trip_id VARCHAR(50) NOT NULL,
    user_id BIGINT,
    trip_title VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    departure_date VARCHAR(100) NOT NULL,
    participant_count INT NOT NULL DEFAULT 1,
    passengers_json TEXT,
    selected_pickup_point VARCHAR(200),
    selected_room_type VARCHAR(100),
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    boarding_pass_qr VARCHAR(150),
    host_name VARCHAR(150),
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_community_trip_category ON manacommunity.community_trip(category);
CREATE INDEX IF NOT EXISTS idx_community_trip_booking_user ON manacommunity.community_trip_booking(user_id);
CREATE INDEX IF NOT EXISTS idx_community_trip_booking_trip ON manacommunity.community_trip_booking(trip_id);