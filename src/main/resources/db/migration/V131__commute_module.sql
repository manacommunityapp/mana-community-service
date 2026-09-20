-- Commute Vehicle table
CREATE TABLE commute_vehicle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    color VARCHAR(30),
    number_plate VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_commute_vehicle_owner FOREIGN KEY (owner_id) REFERENCES app_user(id)
);
CREATE INDEX idx_commute_vehicle_owner ON commute_vehicle(owner_id);

-- Commute Ride table
CREATE TABLE commute_ride (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    from_location VARCHAR(255) NOT NULL,
    to_location VARCHAR(255) NOT NULL,
    from_lat DOUBLE,
    from_lng DOUBLE,
    to_lat DOUBLE,
    to_lng DOUBLE,
    departure_time DATETIME NOT NULL,
    ride_type VARCHAR(20) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    price_per_seat DOUBLE,
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
    distance_km DOUBLE,
    actual_departure_time DATETIME,
    actual_arrival_time DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,
    vehicle_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_commute_ride_driver FOREIGN KEY (driver_id) REFERENCES app_user(id),
    CONSTRAINT fk_commute_ride_community FOREIGN KEY (community_id) REFERENCES community(id),
    CONSTRAINT fk_commute_ride_vehicle FOREIGN KEY (vehicle_id) REFERENCES commute_vehicle(id)
);
CREATE INDEX idx_commute_community_date ON commute_ride(community_id, departure_time DESC);
CREATE INDEX idx_commute_status ON commute_ride(status);
CREATE INDEX idx_commute_driver ON commute_ride(driver_id);
CREATE INDEX idx_commute_type ON commute_ride(ride_type);

-- Commute Booking table
CREATE TABLE commute_booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    seats_booked INT NOT NULL DEFAULT 1,
    pickup_note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_commute_booking_ride FOREIGN KEY (ride_id) REFERENCES commute_ride(id),
    CONSTRAINT fk_commute_booking_passenger FOREIGN KEY (passenger_id) REFERENCES app_user(id),
    CONSTRAINT uk_commute_booking_ride_passenger UNIQUE (ride_id, passenger_id)
);

-- Commute Rating table
CREATE TABLE commute_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    rater_id BIGINT NOT NULL,
    rated_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_commute_rating_ride FOREIGN KEY (ride_id) REFERENCES commute_ride(id),
    CONSTRAINT fk_commute_rating_rater FOREIGN KEY (rater_id) REFERENCES app_user(id),
    CONSTRAINT fk_commute_rating_rated FOREIGN KEY (rated_id) REFERENCES app_user(id),
    CONSTRAINT uk_commute_rating_ride_rater UNIQUE (ride_id, rater_id)
);
CREATE INDEX idx_commute_rating_ride ON commute_rating(ride_id);
CREATE INDEX idx_commute_rating_rated ON commute_rating(rated_id);

-- Commute Favourite Route table
CREATE TABLE commute_favourite_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(60) NOT NULL,
    from_location VARCHAR(255) NOT NULL,
    to_location VARCHAR(255) NOT NULL,
    from_lat DOUBLE,
    from_lng DOUBLE,
    to_lat DOUBLE,
    to_lng DOUBLE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_commute_fav_route_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_commute_fav_user ON commute_favourite_route(user_id);
