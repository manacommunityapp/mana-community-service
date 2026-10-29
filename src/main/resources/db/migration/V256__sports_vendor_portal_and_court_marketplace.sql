-- V256: Sports Vendor Portal, Court Slot Engine, Academy Batches & Sports Marketplace

-- 1. Extend sports_venue with commercial arena attributes
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS sports TEXT;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS amenities TEXT;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS images TEXT;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS is_federated BOOLEAN DEFAULT FALSE;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS partner_communities TEXT;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS rating NUMERIC(3,2) DEFAULT 5.0;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS review_count INT DEFAULT 0;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE sports_venue ADD COLUMN IF NOT EXISTS vendor_id BIGINT;

-- 2. Extend sports_court with sport specs, surfaces, and hourly pricing
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS sport_type VARCHAR(50);
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS surface_type VARCHAR(50);
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS is_indoor BOOLEAN DEFAULT FALSE;
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS has_lighting BOOLEAN DEFAULT TRUE;
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS base_hourly_price NUMERIC(10,2) DEFAULT 400.0;
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS resident_discount_percent NUMERIC(5,2) DEFAULT 10.0;
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS pricing_rules TEXT;
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS rules TEXT;
ALTER TABLE sports_court ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 3. Court Slot Inventory with Hold States
CREATE TABLE IF NOT EXISTS sports_court_slot (
    id BIGSERIAL PRIMARY KEY,
    court_id BIGINT NOT NULL REFERENCES sports_court(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    held_until TIMESTAMP,
    held_by_user_id BIGINT,
    price NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    resident_price NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_court_slot_time UNIQUE (court_id, slot_date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_court_slot_lookup ON sports_court_slot(court_id, slot_date, status);
CREATE INDEX IF NOT EXISTS idx_court_slot_held_expiry ON sports_court_slot(status, held_until);

-- 4. Resident Court Bookings & QR Verification
CREATE TABLE IF NOT EXISTS sports_vendor_booking (
    id BIGSERIAL PRIMARY KEY,
    booking_number VARCHAR(50) NOT NULL UNIQUE,
    venue_id BIGINT NOT NULL REFERENCES sports_venue(id),
    court_id BIGINT NOT NULL REFERENCES sports_court(id),
    slot_id BIGINT NOT NULL REFERENCES sports_court_slot(id),
    resident_id BIGINT NOT NULL,
    resident_name VARCHAR(150) NOT NULL,
    resident_phone VARCHAR(30) NOT NULL,
    resident_flat VARCHAR(50),
    community_name VARCHAR(150),
    booking_date DATE NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID',
    booking_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    qr_code_token VARCHAR(255) NOT NULL UNIQUE,
    checked_in_at TIMESTAMP,
    checked_in_by_user_id BIGINT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendor_booking_resident ON sports_vendor_booking(resident_id);
CREATE INDEX IF NOT EXISTS idx_vendor_booking_venue ON sports_vendor_booking(venue_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_vendor_booking_qr ON sports_vendor_booking(qr_code_token);

-- 5. Sports Academy Coaches
CREATE TABLE IF NOT EXISTS sports_coach (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT,
    vendor_id BIGINT,
    name VARCHAR(150) NOT NULL,
    bio TEXT,
    phone VARCHAR(30),
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    experience_years INT NOT NULL DEFAULT 1,
    sports TEXT NOT NULL DEFAULT '[]',
    certifications TEXT DEFAULT '[]',
    hourly_rate NUMERIC(10,2) NOT NULL DEFAULT 500.0,
    rating NUMERIC(3,2) DEFAULT 5.0,
    review_count INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sports_coach_vendor ON sports_coach(vendor_id);

-- 6. Sports Academy Batches
CREATE TABLE IF NOT EXISTS sports_batch (
    id BIGSERIAL PRIMARY KEY,
    coach_id BIGINT REFERENCES sports_coach(id) ON DELETE SET NULL,
    venue_id BIGINT REFERENCES sports_venue(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    sport VARCHAR(50) NOT NULL,
    skill_level VARCHAR(50) NOT NULL,
    age_group VARCHAR(50) NOT NULL,
    schedule_days TEXT NOT NULL DEFAULT '[]',
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 15,
    current_enrolled INT NOT NULL DEFAULT 0,
    monthly_fee NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sports_batch_venue ON sports_batch(venue_id, is_active);

-- 7. Batch Enrollments & Student Roster
CREATE TABLE IF NOT EXISTS sports_batch_enrollment (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES sports_batch(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(150) NOT NULL,
    parent_name VARCHAR(150),
    parent_phone VARCHAR(30) NOT NULL,
    flat_no VARCHAR(50),
    community_name VARCHAR(150),
    joined_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    fee_paid_until DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_batch_student UNIQUE (batch_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_batch_enrollment_student ON sports_batch_enrollment(student_id);

-- 8. Certified Match Officials (Umpires, Referees, Scorers)
CREATE TABLE IF NOT EXISTS sports_official (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    sport VARCHAR(50) NOT NULL,
    official_role VARCHAR(50) NOT NULL,
    badge_level VARCHAR(50) NOT NULL,
    daily_rate NUMERIC(10,2) NOT NULL DEFAULT 1500.0,
    hourly_rate NUMERIC(10,2) NOT NULL DEFAULT 300.0,
    matches_officiated INT NOT NULL DEFAULT 0,
    rating NUMERIC(3,2) DEFAULT 5.0,
    phone VARCHAR(30),
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_official_sport_role ON sports_official(sport, official_role, is_available);
