-- Migration V57: Create event_pooja_types table and seed default ritual types

CREATE TABLE IF NOT EXISTS event_pooja_types (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pooja_types_community ON event_pooja_types(community_id);
CREATE INDEX IF NOT EXISTS idx_pooja_types_name ON event_pooja_types(name);

-- Seed standard default types if not already present
INSERT INTO event_pooja_types (community_id, name, description, created_at)
SELECT NULL, val.name, 'Default temple ritual', NOW()
FROM (
    VALUES 
        ('Ganesh Puja'),
        ('Ganapati Homam'),
        ('Abhishekam'),
        ('Maha Aarti'),
        ('Satyanarayan Puja'),
        ('Laghu Rudra'),
        ('Navagraha Puja'),
        ('Sahasranama Archana')
) AS val(name)
WHERE NOT EXISTS (
    SELECT 1 FROM event_pooja_types ept WHERE LOWER(ept.name) = LOWER(val.name)
);
