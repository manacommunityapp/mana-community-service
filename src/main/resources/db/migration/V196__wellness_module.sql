-- Wellness Profile
CREATE TABLE manacommunity.wellness_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    height_cm NUMERIC(5,1),
    weight_kg NUMERIC(5,1),
    target_weight_kg NUMERIC(5,1),
    age INTEGER,
    gender VARCHAR(20),
    activity_level VARCHAR(30),
    goal_type VARCHAR(30),
    coaching_style VARCHAR(30),
    daily_calorie_target INTEGER,
    daily_protein_target INTEGER,
    daily_carbs_target INTEGER,
    daily_fat_target INTEGER,
    daily_water_target_ml INTEGER,
    sharing_preference VARCHAR(20) DEFAULT 'PRIVATE',
    dietary_restrictions TEXT,
    allergies TEXT,
    health_conditions TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Meal Log
CREATE TABLE manacommunity.meal_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    meal_date DATE NOT NULL,
    meal_time TIME,
    source_type VARCHAR(20),
    original_input VARCHAR(1000),
    photo_url VARCHAR(500),
    total_calories INTEGER,
    total_protein NUMERIC(8,2),
    total_carbs NUMERIC(8,2),
    total_fat NUMERIC(8,2),
    total_fiber NUMERIC(8,2),
    ai_confidence NUMERIC(4,2),
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_meal_log_user_date ON manacommunity.meal_log(user_id, meal_date);

-- Meal Item
CREATE TABLE manacommunity.meal_item (
    id BIGSERIAL PRIMARY KEY,
    meal_log_id BIGINT NOT NULL REFERENCES manacommunity.meal_log(id) ON DELETE CASCADE,
    food_name VARCHAR(255) NOT NULL,
    food_name_local VARCHAR(255),
    quantity NUMERIC(8,2),
    unit VARCHAR(50),
    calories INTEGER,
    protein NUMERIC(8,2),
    carbs NUMERIC(8,2),
    fat NUMERIC(8,2),
    fiber NUMERIC(8,2),
    food_catalog_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Water Log
CREATE TABLE manacommunity.water_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    log_time TIME,
    amount_ml INTEGER NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_water_log_user_date ON manacommunity.water_log(user_id, log_date);

-- Food Catalog (Indian foods + general)
CREATE TABLE manacommunity.food_catalog (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    category VARCHAR(100),
    cuisine VARCHAR(100),
    serving_size VARCHAR(50),
    serving_unit VARCHAR(50),
    calories_per_serving INTEGER,
    protein_per_serving NUMERIC(8,2),
    carbs_per_serving NUMERIC(8,2),
    fat_per_serving NUMERIC(8,2),
    fiber_per_serving NUMERIC(8,2),
    is_vegetarian BOOLEAN DEFAULT FALSE,
    is_vegan BOOLEAN DEFAULT FALSE,
    region VARCHAR(100),
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Wellness Challenge
CREATE TABLE manacommunity.wellness_challenge (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    challenge_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    target_value INTEGER,
    target_unit VARCHAR(50),
    max_participants INTEGER,
    community_id BIGINT,
    created_by_user_id BIGINT,
    badge_name VARCHAR(100),
    badge_icon VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Challenge Participant
CREATE TABLE manacommunity.challenge_participant (
    id BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL REFERENCES manacommunity.wellness_challenge(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    current_progress INTEGER DEFAULT 0,
    joined_date DATE,
    completed BOOLEAN DEFAULT FALSE,
    completed_date DATE,
    badge_earned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE(challenge_id, user_id)
);

-- Wellness Provider
CREATE TABLE manacommunity.wellness_provider (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider_type VARCHAR(30) NOT NULL,
    specialty VARCHAR(255),
    description TEXT,
    experience_years INTEGER,
    rating NUMERIC(3,1),
    total_reviews INTEGER DEFAULT 0,
    consultation_fee NUMERIC(10,2),
    available_online BOOLEAN DEFAULT FALSE,
    available_in_person BOOLEAN DEFAULT TRUE,
    location VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(255),
    photo_url VARCHAR(500),
    community_id BIGINT,
    verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Accountability Partner
CREATE TABLE manacommunity.accountability_partner (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    partner_user_id BIGINT NOT NULL,
    consent_status VARCHAR(20) DEFAULT 'PENDING',
    consent_type VARCHAR(30),
    started_date DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE(user_id, partner_user_id)
);

-- Seed Indian food catalog
INSERT INTO manacommunity.food_catalog (name, name_local, category, cuisine, serving_size, serving_unit, calories_per_serving, protein_per_serving, carbs_per_serving, fat_per_serving, fiber_per_serving, is_vegetarian, is_vegan, region, verified)
VALUES
('Idli', 'இட்லி', 'Breakfast', 'South Indian', '2', 'pieces', 130, 4.0, 26.0, 1.0, 1.2, TRUE, TRUE, 'South India', TRUE),
('Dosa', 'தோசை', 'Breakfast', 'South Indian', '1', 'piece', 168, 4.0, 28.0, 5.0, 1.5, TRUE, TRUE, 'South India', TRUE),
('Sambar', 'சாம்பார்', 'Side Dish', 'South Indian', '1', 'cup', 130, 6.5, 18.0, 3.5, 4.0, TRUE, TRUE, 'South India', TRUE),
('Dal Tadka', 'दाल तड़का', 'Main Course', 'North Indian', '1', 'cup', 180, 9.0, 24.0, 5.5, 6.0, TRUE, TRUE, 'North India', TRUE),
('Roti', 'रोटी', 'Bread', 'North Indian', '1', 'piece', 120, 3.5, 20.0, 3.5, 2.0, TRUE, TRUE, 'North India', TRUE),
('Chicken Biryani', 'चिकन बिरयानी', 'Main Course', 'Hyderabadi', '1', 'plate', 490, 22.0, 55.0, 18.0, 2.5, FALSE, FALSE, 'South India', TRUE),
('Paneer Butter Masala', 'पनीर बटर मसाला', 'Main Course', 'North Indian', '1', 'cup', 380, 14.0, 18.0, 28.0, 2.0, TRUE, FALSE, 'North India', TRUE),
('Curd Rice', 'தயிர் சாதம்', 'Main Course', 'South Indian', '1', 'cup', 210, 6.0, 32.0, 6.5, 0.5, TRUE, FALSE, 'South India', TRUE),
('Poha', 'पोहा', 'Breakfast', 'Maharashtrian', '1', 'plate', 250, 5.5, 40.0, 7.0, 2.5, TRUE, TRUE, 'West India', TRUE),
('Upma', 'உப்புமா', 'Breakfast', 'South Indian', '1', 'cup', 210, 5.0, 32.0, 7.0, 3.0, TRUE, TRUE, 'South India', TRUE),
('Masala Chai', 'मसाला चाय', 'Beverage', 'Indian', '1', 'cup', 100, 3.0, 12.0, 4.0, 0.0, TRUE, FALSE, 'All India', TRUE),
('Lassi', 'लस्सी', 'Beverage', 'Punjabi', '1', 'glass', 150, 5.0, 22.0, 4.5, 0.0, TRUE, FALSE, 'North India', TRUE),
('Rajma Chawal', 'राजमा चावल', 'Main Course', 'North Indian', '1', 'plate', 350, 12.0, 55.0, 8.0, 8.0, TRUE, TRUE, 'North India', TRUE),
('Pav Bhaji', 'पाव भाजी', 'Street Food', 'Maharashtrian', '1', 'plate', 400, 10.0, 52.0, 16.0, 5.0, TRUE, FALSE, 'West India', TRUE),
('Vada Pav', 'वडा पाव', 'Street Food', 'Maharashtrian', '1', 'piece', 290, 5.5, 38.0, 12.0, 3.0, TRUE, TRUE, 'West India', TRUE);
