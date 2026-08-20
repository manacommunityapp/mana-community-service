-- Migration V61: Create community_designations table and seed default designations

CREATE TABLE IF NOT EXISTS community_designations (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comm_designations_community ON community_designations(community_id);
CREATE INDEX IF NOT EXISTS idx_comm_designations_name ON community_designations(name);

-- Seed standard default designations
INSERT INTO community_designations (community_id, name, display_order, is_default, created_at)
SELECT NULL, val.name, val.ord, TRUE, NOW()
FROM (
    VALUES 
        ('President', 1),
        ('Vice President', 2),
        ('Secretary', 3),
        ('Joint Secretary', 4),
        ('Treasurer', 5),
        ('Sports Director', 6),
        ('Director', 7),
        ('Chairman', 8),
        ('Chairperson', 9),
        ('Cultural Head', 10),
        ('Maintenance Head', 11),
        ('Security Head', 12),
        ('Grievance Officer', 13),
        ('Committee Member', 14),
        ('Member', 15)
) AS val(name, ord)
WHERE NOT EXISTS (
    SELECT 1 FROM community_designations cd WHERE LOWER(cd.name) = LOWER(val.name)
);
