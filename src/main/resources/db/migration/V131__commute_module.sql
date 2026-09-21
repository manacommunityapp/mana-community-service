-- Commute Vehicle table
CREATE TABLE IF NOT EXISTS manacommunity.commute_vehicle (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    color VARCHAR(30),
    number_plate VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_commute_vehicle_owner FOREIGN KEY (owner_id) REFERENCES manacommunity.app_user(id)
);
CREATE INDEX IF NOT EXISTS idx_commute_vehicle_owner ON manacommunity.commute_vehicle(owner_id);

-- Commute Ride table
CREATE TABLE IF NOT EXISTS manacommunity.commute_ride (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    from_location VARCHAR(255) NOT NULL,
    to_location VARCHAR(255) NOT NULL,
    from_lat DOUBLE PRECISION,
    from_lng DOUBLE PRECISION,
    to_lat DOUBLE PRECISION,
    to_lng DOUBLE PRECISION,
    departure_time TIMESTAMP NOT NULL,
    ride_type VARCHAR(20) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    price_per_seat DOUBLE PRECISION,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    vehicle_type VARCHAR(50),
    vehicle_number VARCHAR(20),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_days VARCHAR(50),
    recurring_time TIME,
    ladies_only BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    distance_km DOUBLE PRECISION,
    actual_departure_time TIMESTAMP,
    actual_arrival_time TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    vehicle_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_commute_ride_driver FOREIGN KEY (driver_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_commute_ride_community FOREIGN KEY (community_id) REFERENCES manacommunity.community(id),
    CONSTRAINT fk_commute_ride_vehicle FOREIGN KEY (vehicle_id) REFERENCES manacommunity.commute_vehicle(id)
);
CREATE INDEX IF NOT EXISTS idx_commute_community_date ON manacommunity.commute_ride(community_id, departure_time DESC);
CREATE INDEX IF NOT EXISTS idx_commute_status ON manacommunity.commute_ride(status);
CREATE INDEX IF NOT EXISTS idx_commute_driver ON manacommunity.commute_ride(driver_id);
CREATE INDEX IF NOT EXISTS idx_commute_type ON manacommunity.commute_ride(ride_type);

-- Commute Booking table
CREATE TABLE IF NOT EXISTS manacommunity.commute_booking (
    id BIGSERIAL PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    seats_booked INT NOT NULL DEFAULT 1,
    pickup_note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_commute_booking_ride FOREIGN KEY (ride_id) REFERENCES manacommunity.commute_ride(id),
    CONSTRAINT fk_commute_booking_passenger FOREIGN KEY (passenger_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT uk_commute_booking_ride_passenger UNIQUE (ride_id, passenger_id)
);
CREATE INDEX IF NOT EXISTS idx_booking_ride ON manacommunity.commute_booking(ride_id);
CREATE INDEX IF NOT EXISTS idx_booking_passenger ON manacommunity.commute_booking(passenger_id);
CREATE INDEX IF NOT EXISTS idx_booking_status ON manacommunity.commute_booking(status);

-- Commute Rating table
CREATE TABLE IF NOT EXISTS manacommunity.commute_rating (
    id BIGSERIAL PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    rater_id BIGINT NOT NULL,
    rated_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_commute_rating_ride FOREIGN KEY (ride_id) REFERENCES manacommunity.commute_ride(id),
    CONSTRAINT fk_commute_rating_rater FOREIGN KEY (rater_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT fk_commute_rating_rated FOREIGN KEY (rated_id) REFERENCES manacommunity.app_user(id),
    CONSTRAINT uk_commute_rating_ride_rater UNIQUE (ride_id, rater_id)
);
CREATE INDEX IF NOT EXISTS idx_commute_rating_ride ON manacommunity.commute_rating(ride_id);
CREATE INDEX IF NOT EXISTS idx_commute_rating_rated ON manacommunity.commute_rating(rated_id);

-- Commute Favourite Route table
CREATE TABLE IF NOT EXISTS manacommunity.commute_favourite_route (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(60) NOT NULL,
    from_location VARCHAR(255) NOT NULL,
    to_location VARCHAR(255) NOT NULL,
    from_lat DOUBLE PRECISION,
    from_lng DOUBLE PRECISION,
    to_lat DOUBLE PRECISION,
    to_lng DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_commute_fav_route_user FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id)
);
CREATE INDEX IF NOT EXISTS idx_commute_fav_user ON manacommunity.commute_favourite_route(user_id);
