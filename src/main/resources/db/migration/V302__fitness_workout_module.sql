-- ══════════════════════════════════════════════════════════════
-- V238  Fitness Workout Module — Phase 1
-- exercise_catalog + workout_log
-- ══════════════════════════════════════════════════════════════

SET search_path = manacommunity;

-- ── Exercise catalog ──────────────────────────────────────────
CREATE TABLE exercise_catalog (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    name_local        VARCHAR(100),
    category          VARCHAR(30)  NOT NULL,  -- CARDIO / STRENGTH / FLEXIBILITY / YOGA / SPORTS / HIIT / BALANCE
    description       VARCHAR(500),
    met_value         NUMERIC(5,2) NOT NULL,  -- Metabolic Equivalent; kcal = MET × kg × hours
    muscles_targeted  TEXT,                   -- JSON array e.g. '["quads","glutes"]'
    is_equipment_needed BOOLEAN DEFAULT FALSE,
    is_bodyweight     BOOLEAN DEFAULT TRUE,
    verified          BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_exercise_catalog_category ON exercise_catalog(category);
CREATE INDEX idx_exercise_catalog_name ON exercise_catalog(LOWER(name));

-- ── Seed: 25 exercises across categories with MET values ──────
INSERT INTO exercise_catalog (name, name_local, category, met_value, muscles_targeted, is_bodyweight, is_equipment_needed, verified) VALUES
-- Cardio
('Running',            'दौड़ना',       'CARDIO',     8.0,  '["quads","calves","hip_flexors"]',    true,  false, true),
('Brisk Walking',      'तेज़ चलना',    'CARDIO',     3.5,  '["quads","calves"]',                  true,  false, true),
('Cycling',            'साइकिलिंग',   'CARDIO',     7.5,  '["quads","hamstrings","glutes"]',     false, true,  true),
('Swimming',           'तैराकी',       'CARDIO',     6.0,  '["full_body"]',                       true,  false, true),
('Jump Rope',          'रस्सी कूदना',  'CARDIO',     10.0, '["calves","shoulders","core"]',       false, true,  true),
('Stair Climbing',     'सीढ़ी चढ़ना',   'CARDIO',     8.0,  '["quads","glutes","calves"]',         true,  false, true),
-- Strength
('Push-ups',           'पुश-अप्स',    'STRENGTH',   3.8,  '["chest","triceps","shoulders"]',     true,  false, true),
('Pull-ups',           'पुल-अप्स',    'STRENGTH',   4.0,  '["back","biceps"]',                   true,  false, true),
('Squats',             'स्क्वाट',      'STRENGTH',   5.0,  '["quads","glutes","hamstrings"]',     true,  false, true),
('Deadlift',           'डेडलिफ्ट',    'STRENGTH',   6.0,  '["back","glutes","hamstrings"]',      false, true,  true),
('Plank',              'प्लैंक',       'STRENGTH',   3.0,  '["core","shoulders"]',                true,  false, true),
('Dumbbell Curls',     'डंबल कर्ल',   'STRENGTH',   3.5,  '["biceps"]',                          false, true,  true),
-- HIIT
('Burpees',            'बर्पीज़',      'HIIT',       8.0,  '["full_body"]',                       true,  false, true),
('Mountain Climbers',  'माउंटेन क्लाइमर', 'HIIT',  8.0,  '["core","shoulders","quads"]',        true,  false, true),
('Box Jumps',          'बॉक्स जंप',   'HIIT',       7.0,  '["quads","glutes","calves"]',         true,  false, true),
-- Yoga
('Surya Namaskar',     'सूर्य नमस्कार', 'YOGA',     3.3,  '["full_body"]',                       true,  false, true),
('Pranayama',          'प्राणायाम',    'YOGA',       2.5,  '["respiratory"]',                     true,  false, true),
('Power Yoga',         'पावर योग',    'YOGA',       4.0,  '["full_body","core"]',                true,  false, true),
-- Sports
('Badminton',          'बैडमिंटन',    'SPORTS',     5.5,  '["shoulders","legs","core"]',         false, true,  true),
('Cricket',            'क्रिकेट',      'SPORTS',     4.8,  '["full_body"]',                       false, true,  true),
('Football',           'फुटबॉल',      'SPORTS',     7.0,  '["quads","calves","core"]',           false, false, true),
('Basketball',         'बास्केटबॉल',  'SPORTS',     6.5,  '["full_body"]',                       false, false, true),
-- Flexibility
('Stretching',         'स्ट्रेचिंग',   'FLEXIBILITY', 2.5, '["full_body"]',                      true,  false, true),
('Foam Rolling',       'फोम रोलिंग',  'FLEXIBILITY', 2.0, '["full_body"]',                       false, true,  true),
-- Balance
('Tai Chi',            'ताई ची',       'BALANCE',    3.0,  '["full_body","core"]',                true,  false, true);

-- ── Workout log ───────────────────────────────────────────────
CREATE TABLE workout_log (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    exercise_catalog_id     BIGINT REFERENCES exercise_catalog(id) ON DELETE SET NULL,
    exercise_name           VARCHAR(100) NOT NULL,
    workout_date            DATE NOT NULL DEFAULT CURRENT_DATE,
    start_time              TIME,
    duration_min            INTEGER,
    calories_burned         INTEGER,                        -- computed or manual
    sets                    INTEGER,
    reps_per_set            INTEGER,
    weight_kg               NUMERIC(6,2),                  -- for strength training
    distance_km             NUMERIC(7,3),                  -- for cardio
    source                  VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL / WEARABLE / AI_TEXT / SPORTS_BOOKING
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_workout_log_user_date ON workout_log(user_id, workout_date DESC);
CREATE INDEX idx_workout_log_user_id ON workout_log(user_id);
