-- V244: Fit profiles and jewellery recommendation engine

CREATE TABLE fit_profiles (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    face_shape  VARCHAR(20) NOT NULL
                CHECK (face_shape IN ('round','oval','square','heart','oblong','unknown')),
    width_ratio NUMERIC(4,3),
    neck_ratio  NUMERIC(4,3),
    confidence  NUMERIC(3,2) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product_fit_attributes (
    product_id         BIGINT PRIMARY KEY REFERENCES jewellery_product(id) ON DELETE CASCADE,
    drop_length_mm     INT,
    size_mm            INT,
    necklace_length_in NUMERIC(4,1),
    style_tags         TEXT[] NOT NULL DEFAULT '{}'
);

CREATE TABLE recommendation_rules (
    id         BIGSERIAL PRIMARY KEY,
    category   VARCHAR(30) NOT NULL,
    face_shape VARCHAR(20) NOT NULL,
    attribute  VARCHAR(30) NOT NULL
               CHECK (attribute IN ('drop_length_mm','size_mm','necklace_length_in')),
    min_value  NUMERIC,
    max_value  NUMERIC,
    weight     NUMERIC(4,2) NOT NULL DEFAULT 1.0,
    active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_rules_lookup ON recommendation_rules (category, face_shape) WHERE active;

INSERT INTO recommendation_rules (category, face_shape, attribute, min_value, max_value, weight) VALUES
    ('EARRINGS', 'round',  'drop_length_mm', 30, 70, 1.0),
    ('EARRINGS', 'square', 'drop_length_mm', 15, 40, 1.0),
    ('EARRINGS', 'oval',   'drop_length_mm', 10, 60, 1.0),
    ('EARRINGS', 'heart',  'drop_length_mm', 20, 50, 1.0),
    ('EARRINGS', 'oblong', 'drop_length_mm', 5,  30, 1.0),
    ('NECKLACE', 'round',  'necklace_length_in', 18, 30, 1.0),
    ('NECKLACE', 'square', 'necklace_length_in', 16, 24, 1.0),
    ('NECKLACE', 'oval',   'necklace_length_in', 14, 28, 1.0),
    ('NECKLACE', 'heart',  'necklace_length_in', 14, 20, 1.0),
    ('NECKLACE', 'oblong', 'necklace_length_in', 14, 18, 1.0);
