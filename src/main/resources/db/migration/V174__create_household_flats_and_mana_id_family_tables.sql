-- ============================================================================
-- V174__create_household_flats_and_mana_id_family_tables.sql
-- Mana ID Multi-User Single-Flat Household & Resident Architecture
-- ============================================================================

-- 1. Community Flats (Household unit entity)
CREATE TABLE IF NOT EXISTS manacommunity.community_flat (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    tower_block         VARCHAR(50) NOT NULL,
    flat_number         VARCHAR(50) NOT NULL,
    floor_number        INT,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
    verified_by         BIGINT,
    verified_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_community_tower_flat UNIQUE (community_id, tower_block, flat_number)
);

CREATE INDEX IF NOT EXISTS idx_community_flat_lookup ON manacommunity.community_flat(community_id, tower_block, flat_number);
CREATE INDEX IF NOT EXISTS idx_community_flat_status ON manacommunity.community_flat(verification_status);

-- 2. Flat Memberships (Adults / Users linked to Flats - Many-to-Many)
CREATE TABLE IF NOT EXISTS manacommunity.flat_membership (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    flat_id             BIGINT NOT NULL REFERENCES manacommunity.community_flat(id) ON DELETE CASCADE,
    resident_type       VARCHAR(30) NOT NULL DEFAULT 'OWNER', -- OWNER, TENANT, FAMILY_MEMBER
    relationship_to_flat VARCHAR(50) DEFAULT 'SELF',          -- SELF, SPOUSE, PARENT, CHILD, SIBLING, RELATIVE, OTHER
    is_primary_resident BOOLEAN NOT NULL DEFAULT FALSE,
    app_access_status   VARCHAR(30) NOT NULL DEFAULT 'FULL_ACCESS', -- FULL_ACCESS, READ_ONLY, INVITED, REVOKED
    invite_phone        VARCHAR(20),
    invited_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_flat_membership UNIQUE (user_id, flat_id)
);

CREATE INDEX IF NOT EXISTS idx_flat_membership_user ON manacommunity.flat_membership(user_id);
CREATE INDEX IF NOT EXISTS idx_flat_membership_flat ON manacommunity.flat_membership(flat_id);
CREATE INDEX IF NOT EXISTS idx_flat_membership_primary ON manacommunity.flat_membership(flat_id, is_primary_resident);

-- 3. Flat Dependent Members (Children / Non-Login Family Members managed by Guardians)
CREATE TABLE IF NOT EXISTS manacommunity.flat_dependent_member (
    id                  BIGSERIAL PRIMARY KEY,
    flat_id             BIGINT NOT NULL REFERENCES manacommunity.community_flat(id) ON DELETE CASCADE,
    guardian_user_id    BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    full_name           VARCHAR(150) NOT NULL,
    date_of_birth       DATE NOT NULL,
    gender              VARCHAR(20) NOT NULL,                    -- MALE, FEMALE, OTHER
    relationship        VARCHAR(50) NOT NULL,                    -- SON, DAUGHTER, DEPENDENT_PARENT, OTHER
    is_parent_managed   BOOLEAN NOT NULL DEFAULT TRUE,
    emergency_contact   VARCHAR(20),
    blood_group         VARCHAR(20),
    profile_pic_url     TEXT,
    sports_eligible     BOOLEAN DEFAULT TRUE,
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE, INACTIVE, ARCHIVED
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_flat_dependent_flat ON manacommunity.flat_dependent_member(flat_id);
CREATE INDEX IF NOT EXISTS idx_flat_dependent_guardian ON manacommunity.flat_dependent_member(guardian_user_id);
CREATE INDEX IF NOT EXISTS idx_flat_dependent_dob ON manacommunity.flat_dependent_member(date_of_birth);

-- 4. Non-Destructive Data Backfill for Existing Users
INSERT INTO manacommunity.community_flat (community_id, tower_block, flat_number, verification_status, created_at, updated_at)
SELECT DISTINCT
    u.community_id,
    COALESCE(NULLIF(TRIM(u.block), ''), NULLIF(TRIM(u.tower), ''), 'Tower A') AS tower_block,
    TRIM(u.flat_no) AS flat_number,
    CASE WHEN u.kyc_status = 'VERIFIED' THEN 'VERIFIED' ELSE 'PENDING' END AS verification_status,
    NOW(),
    NOW()
FROM manacommunity.app_user u
WHERE u.community_id IS NOT NULL 
  AND u.flat_no IS NOT NULL 
  AND TRIM(u.flat_no) <> ''
ON CONFLICT (community_id, tower_block, flat_number) DO NOTHING;

INSERT INTO manacommunity.flat_membership (user_id, flat_id, resident_type, relationship_to_flat, is_primary_resident, app_access_status, created_at, updated_at)
SELECT 
    u.id AS user_id,
    cf.id AS flat_id,
    CASE 
        WHEN UPPER(TRIM(COALESCE(u.occupancy_status, ''))) = 'TENANT' THEN 'TENANT'
        WHEN UPPER(TRIM(COALESCE(u.occupancy_status, ''))) = 'FAMILY_MEMBER' THEN 'FAMILY_MEMBER'
        ELSE 'OWNER'
    END AS resident_type,
    'SELF' AS relationship_to_flat,
    TRUE AS is_primary_resident,
    'FULL_ACCESS' AS app_access_status,
    NOW(),
    NOW()
FROM manacommunity.app_user u
JOIN manacommunity.community_flat cf ON cf.community_id = u.community_id 
    AND cf.tower_block = COALESCE(NULLIF(TRIM(u.block), ''), NULLIF(TRIM(u.tower), ''), 'Tower A')
    AND cf.flat_number = TRIM(u.flat_no)
ON CONFLICT (user_id, flat_id) DO NOTHING;
