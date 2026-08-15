-- V51__app_user_roles_join_table.sql
-- Introduces a proper many-to-many join table between app_user and roles.
-- The existing comma-separated app_user.role column is kept as a
-- convenience denormalised column (read-only mirror, maintained by the app).
--
-- Migration strategy (backward-safe):
--   1. Create app_user_roles.
--   2. Back-fill from the existing app_user.role string — for each user we
--      split the comma-separated value and resolve each role name against the
--      roles table (or insert a community-scoped row if not yet present).
--   3. The original app_user.role column is kept and kept in sync by the app.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Create the join table.
CREATE TABLE IF NOT EXISTS app_user_roles (
    user_id BIGINT       NOT NULL,
    role_id BIGINT       NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_app_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_aur_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_aur_role FOREIGN KEY (role_id) REFERENCES roles (id)       ON DELETE CASCADE
);

-- Index for reverse lookup: "find all users that have role X"
CREATE INDEX IF NOT EXISTS idx_app_user_roles_role_id ON app_user_roles (role_id);

-- 2. Back-fill from app_user.role (comma-separated string).
--    For each non-null role segment, resolve or create the role row, then
--    insert into app_user_roles (IGNORE duplicate PK to make the script idempotent).
DO $$
DECLARE
    rec         RECORD;
    role_segment TEXT;
    role_parts   TEXT[];
    resolved_id  BIGINT;
BEGIN
    FOR rec IN
        SELECT u.id AS user_id,
               u.role AS role_str,
               COALESCE(u.community_id, NULL) AS community_id
        FROM   app_user u
        WHERE  u.role IS NOT NULL AND u.role <> ''
    LOOP
        -- Split the comma-separated role string.
        role_parts := string_to_array(rec.role_str, ',');

        FOREACH role_segment IN ARRAY role_parts LOOP
            role_segment := upper(trim(role_segment));
            CONTINUE WHEN role_segment = '';

            -- Resolve: community-scoped first, then global.
            SELECT id INTO resolved_id
            FROM   roles
            WHERE  upper(name) = role_segment
              AND  (community_id = rec.community_id
                    OR (rec.community_id IS NULL AND community_id IS NULL))
            ORDER  BY (community_id IS NOT NULL) DESC   -- prefer community-scoped
            LIMIT  1;

            -- If no matching role exists yet, create a global one.
            IF resolved_id IS NULL THEN
                INSERT INTO roles (name, community_id)
                VALUES (role_segment, rec.community_id)
                ON CONFLICT DO NOTHING
                RETURNING id INTO resolved_id;

                -- If ON CONFLICT suppressed the insert, fetch the existing id.
                IF resolved_id IS NULL THEN
                    SELECT id INTO resolved_id
                    FROM   roles
                    WHERE  upper(name) = role_segment
                      AND  (community_id = rec.community_id
                            OR (rec.community_id IS NULL AND community_id IS NULL))
                    LIMIT  1;
                END IF;
            END IF;

            -- Insert the join row (skip if already present — idempotent).
            IF resolved_id IS NOT NULL THEN
                INSERT INTO app_user_roles (user_id, role_id)
                VALUES (rec.user_id, resolved_id)
                ON CONFLICT (user_id, role_id) DO NOTHING;
            END IF;

        END LOOP;
    END LOOP;
END;
$$;

COMMENT ON TABLE app_user_roles IS
    'Many-to-many join: one user may hold multiple roles. '
    'Kept in sync with app_user.role (comma string) by the application layer.';
COMMENT ON COLUMN app_user_roles.assigned_at IS
    'Timestamp when the role was assigned to the user.';
