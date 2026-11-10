-- =============================================================================
-- delete_user.sql  --  remove ONE user (manacommunity.app_user) from PostgreSQL
-- =============================================================================
-- NOT a Flyway migration (lives in scripts/db, not src/main/resources/db).
-- Run it by hand with psql / pgAdmin / DBeaver against the target database.

-- ============================================================================
-- ENVIRONMENT SAFETY GUARD: STRICTLY LOCAL DATABASE ONLY
-- Prohibits execution on AWS Lightsail, AWS RDS, Dev, UAT, and Prod databases.
-- ============================================================================
DO $$
DECLARE
    v_server_addr inet := inet_server_addr();
    v_allow_reset text := current_setting('app.allow_local_reset', true);
BEGIN
    -- 1. STRICT REMOTE SERVER BLOCK:
    -- inet_server_addr() is NULL for local unix socket, or '127.0.0.1' / '::1' for localhost loopback.
    -- If connected over network to any non-loopback IP (e.g. AWS Lightsail 3.109.145.130 or VPC 172.x), ABORT IMMEDIATELY.
    IF v_server_addr IS NOT NULL AND v_server_addr NOT IN ('127.0.0.1'::inet, '::1'::inet) THEN
        RAISE EXCEPTION 'CRITICAL ENVIRONMENT SAFETY ERROR: Destructive user deletion scripts are strictly prohibited on remote/AWS servers! Detected database server IP: %. Execution ABORTED.', v_server_addr;
    END IF;

    -- 2. EXPLICIT LOCAL CONFIRMATION GUARD:
    -- Prevent accidental execution. Must run `SET app.allow_local_reset = 'true';` first.
    IF v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'SAFETY GUARD: This script will delete user data and is restricted to LOCAL environments only. To run on your LOCAL database, you must first execute in this session: SET app.allow_local_reset = ''true'';';
    END IF;
END $$;
--
-- WHY A PLAIN "DELETE FROM app_user WHERE id = ?" DOES NOT WORK
--   More than 100 foreign keys point at app_user(id). Many are NO ACTION (no
--   ON DELETE rule), so the delete fails with SQLSTATE 23503 as soon as the
--   user owns a single wallet / order / invoice / booking / registration row.
--   The child tables have children of their own (order -> order items, ...).
--
-- WHAT THIS SCRIPT DOES (reads the LIVE catalog, so nothing is hard-coded)
--   For every FK that points at the row being removed, recursively:
--     * FK ON DELETE SET NULL / SET DEFAULT   -> left to the database
--     * FK column(s) NOT NULL, or ON DELETE CASCADE
--                                             -> row is owned by the user:
--                                                its children are removed first,
--                                                then the row itself
--     * FK column nullable, NO ACTION/RESTRICT (created_by, approved_by,
--       resolved_by, seller_id, ...)          -> attribution only: the column
--                                                is set to NULL and the row is
--                                                KEPT (it belongs to someone else)
--   Then it removes rows that carry the id WITHOUT an FK (Envers history
--   app_user_aud, user_privacy_settings) and finally the app_user row.
--
-- SAFE BY DEFAULT: section 5 runs inside a transaction that ends in ROLLBACK,
-- printing a row count per table. Read the output, then switch to COMMIT.
--
-- STATUS: written from the entities and Flyway migrations. It was NOT executed
-- (no PostgreSQL in the authoring environment). Dry-run it on a copy or staging
-- database before you run it on production.
-- =============================================================================


-- =============================================================================
-- 1. FIND THE USER  (read-only) -- note the id you want
-- =============================================================================
SELECT id, full_name, email, phone, role, community_id, is_active, created_at
FROM   manacommunity.app_user
WHERE  email ILIKE '%someone@example.com%'      -- edit one of these three
   OR  phone = '9999999999'
   OR  id    = -1
ORDER  BY id;


-- =============================================================================
-- 2. INVENTORY OF EVERY FK THAT POINTS AT app_user  (read-only)
--    owned = TRUE  -> rows are deleted by this script
--    owned = FALSE -> (NO ACTION / RESTRICT) column is set to NULL, row kept
--    on_delete SET NULL / SET DEFAULT are handled by the database itself
-- =============================================================================
SELECT c.conrelid::regclass                                   AS referencing_table,
       string_agg(a.attname, ', ' ORDER BY k.ord)             AS fk_columns,
       CASE c.confdeltype WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT'
                          WHEN 'c' THEN 'CASCADE'   WHEN 'n' THEN 'SET NULL'
                          WHEN 'd' THEN 'SET DEFAULT' END     AS on_delete,
       bool_and(a.attnotnull)                                 AS columns_not_null,
       (bool_and(a.attnotnull) OR c.confdeltype = 'c')        AS owned_rows_deleted
FROM   pg_constraint c
CROSS  JOIN LATERAL unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
JOIN   pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum
WHERE  c.contype   = 'f'
  AND  c.confrelid = 'manacommunity.app_user'::regclass
GROUP  BY c.oid, c.conrelid, c.confdeltype
ORDER  BY owned_rows_deleted DESC, referencing_table::text;


-- =============================================================================
-- 3. COLUMNS THAT LOOK LIKE A USER ID BUT HAVE NO FK  (read-only, review by hand)
--    The FK walk cannot see these. Dangling ids stay behind, and some tables may
--    hold personal data. Add any that matter to the "no-FK cleanup" in section 4.
-- =============================================================================
SELECT c.table_name, c.column_name, c.data_type
FROM   information_schema.columns c
JOIN   information_schema.tables  t
       ON t.table_schema = c.table_schema AND t.table_name = c.table_name
WHERE  c.table_schema = 'manacommunity'
  AND  t.table_type   = 'BASE TABLE'
  AND  c.data_type IN ('bigint', 'integer')
  AND (   c.column_name ~ '(^|_)(user|owner|resident|member|author|reporter|creator|approver|assignee)_?id$'
       OR c.column_name ~ '_by(_id|_user_id)?$')
  AND  c.table_name <> 'app_user'
  AND  NOT EXISTS (
         SELECT 1
         FROM   pg_constraint k
         JOIN   pg_attribute  pa ON pa.attrelid = k.conrelid AND pa.attnum = k.conkey[1]
         WHERE  k.contype   = 'f'
           AND  k.confrelid = 'manacommunity.app_user'::regclass
           AND  k.conrelid  = format('%I.%I', c.table_schema, c.table_name)::regclass
           AND  pa.attname  = c.column_name)
ORDER  BY c.table_name, c.column_name;


-- =============================================================================
-- 4. HELPER FUNCTIONS  (created in pg_temp: they vanish when the session ends)
-- =============================================================================
CREATE OR REPLACE FUNCTION pg_temp.purge_refs(
    p_parent    regclass,   -- table whose rows are being removed
    p_pred      text,       -- SQL predicate that selects those rows
    p_depth     int,
    p_protected text[]      -- table names that must never be deleted from
) RETURNS void
LANGUAGE plpgsql AS $fn$
DECLARE
    fk          record;
    child_tbl   text;
    child_pred  text;
    v_rel       text;
    v_owned     boolean;
    n           bigint;
    pad         text := repeat('  ', p_depth);
BEGIN
    IF p_depth > 8 THEN
        RAISE EXCEPTION 'FK chain deeper than 8 levels under % (possible cycle) - aborting', p_parent;
    END IF;

    FOR fk IN
        SELECT c.conrelid::regclass AS child,
               c.conrelid           AS child_oid,
               c.confdeltype        AS del_rule,
               (SELECT string_agg(quote_ident(a.attname), ', ' ORDER BY k.ord)
                  FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
                  JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum) AS child_cols,
               (SELECT string_agg(quote_ident(a.attname), ', ' ORDER BY k.ord)
                  FROM unnest(c.confkey) WITH ORDINALITY AS k(attnum, ord)
                  JOIN pg_attribute a ON a.attrelid = c.confrelid AND a.attnum = k.attnum) AS parent_cols,
               (SELECT string_agg(quote_ident(a.attname) || ' = NULL', ', ' ORDER BY k.ord)
                  FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
                  JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum) AS null_set,
               (SELECT bool_and(a.attnotnull)
                  FROM unnest(c.conkey) AS k(attnum)
                  JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum) AS cols_not_null
        FROM   pg_constraint c
        WHERE  c.contype   = 'f'
          AND  c.confrelid = p_parent
          AND  c.confdeltype NOT IN ('n', 'd')      -- the database handles SET NULL / SET DEFAULT
        ORDER  BY c.conrelid::regclass::text
    LOOP
        child_tbl  := fk.child::text;
        child_pred := format('(%s) IN (SELECT %s FROM %s WHERE %s)',
                             fk.child_cols, fk.parent_cols, p_parent::text, p_pred);

        EXECUTE format('SELECT count(*) FROM %s WHERE %s', child_tbl, child_pred) INTO n;
        CONTINUE WHEN n = 0;                         -- nothing here, so nothing deeper either

        v_owned := fk.cols_not_null OR fk.del_rule = 'c';

        IF v_owned THEN
            IF fk.child = p_parent THEN              -- self reference that cannot be nulled
                RAISE NOTICE '%SKIP % : NOT NULL self-reference, handle manually', pad, child_tbl;
                CONTINUE;
            END IF;

            v_rel := (SELECT relname::text FROM pg_class WHERE oid = fk.child_oid);
            IF v_rel = ANY (p_protected) THEN
                RAISE EXCEPTION 'Table % is protected but holds % row(s) owned by this user - anonymise the user instead of deleting',
                                child_tbl, n;
            END IF;

            PERFORM pg_temp.purge_refs(fk.child, child_pred, p_depth + 1, p_protected);

            EXECUTE format('DELETE FROM %s WHERE %s', child_tbl, child_pred);
            GET DIAGNOSTICS n = ROW_COUNT;
            RAISE NOTICE '%DELETE  % row(s) from %', pad, n, child_tbl;
        ELSE
            EXECUTE format('UPDATE %s SET %s WHERE %s', child_tbl, fk.null_set, child_pred);
            GET DIAGNOSTICS n = ROW_COUNT;
            RAISE NOTICE '%NULLIFY % row(s) in % (%)', pad, n, child_tbl, fk.child_cols;
        END IF;
    END LOOP;
END
$fn$;


CREATE OR REPLACE FUNCTION pg_temp.delete_user(
    p_user_id     bigint,
    p_protected   text[]  DEFAULT '{}',    -- e.g. ARRAY['invoice','payment_order']
    p_allow_admin boolean DEFAULT false    -- must be true to delete a SUPER_ADMIN
) RETURNS void
LANGUAGE plpgsql AS $fn$
DECLARE
    u record;
    n bigint;
BEGIN
    -- Strict environment guard
    IF (inet_server_addr() IS NOT NULL AND inet_server_addr() NOT IN ('127.0.0.1'::inet, '::1'::inet)) OR current_setting('app.allow_local_reset', true) IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'CRITICAL BLOCKED: delete_user can ONLY run on a LOCAL database (127.0.0.1) with SET app.allow_local_reset = ''true''; (detected server: %)', inet_server_addr();
    END IF;

    SELECT id, email, role INTO u FROM manacommunity.app_user WHERE id = p_user_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'User % not found', p_user_id;
    END IF;
    IF NOT p_allow_admin AND u.role ILIKE '%SUPER_ADMIN%' THEN
        RAISE EXCEPTION 'User % (%) is a SUPER_ADMIN - pass p_allow_admin => true to delete', u.id, u.email;
    END IF;

    RAISE NOTICE 'Removing user % (%)', u.id, u.email;

    -- 1. everything reachable through a foreign key
    PERFORM pg_temp.purge_refs('manacommunity.app_user'::regclass,
                               format('id = %s', p_user_id), 1, p_protected);

    -- 2. tables that hold the user id WITHOUT a foreign key.
    --    Add more (table, column) pairs here after reviewing section 3.
    --    data_deletion_request is kept on purpose: it is the proof of erasure.
    IF to_regclass('manacommunity.user_privacy_settings') IS NOT NULL THEN
        DELETE FROM manacommunity.user_privacy_settings WHERE user_id = p_user_id;
        GET DIAGNOSTICS n = ROW_COUNT;
        RAISE NOTICE 'DELETE  % row(s) from user_privacy_settings (no FK)', n;
    END IF;
    IF to_regclass('manacommunity.app_user_aud') IS NOT NULL THEN   -- Envers history keeps old name/email/phone
        DELETE FROM manacommunity.app_user_aud WHERE id = p_user_id;
        GET DIAGNOSTICS n = ROW_COUNT;
        RAISE NOTICE 'DELETE  % row(s) from app_user_aud (no FK)', n;
    END IF;

    -- 3. the user row itself
    DELETE FROM manacommunity.app_user WHERE id = p_user_id;
    GET DIAGNOSTICS n = ROW_COUNT;
    RAISE NOTICE 'DELETE  % row(s) from app_user', n;
END
$fn$;


-- =============================================================================
-- 5. RUN IT  --  dry run first
--    The NOTICE lines (Messages tab in pgAdmin/DBeaver) list every table touched.
--    If it fails with an exception, run ROLLBACK; and fix the cause.
--    Happy with the output? Change ROLLBACK to COMMIT and run again.
-- =============================================================================
BEGIN;

SELECT pg_temp.delete_user(
    12345,                                   -- <-- app_user.id from section 1
    ARRAY[]::text[]                          -- tables to protect, e.g. ARRAY['invoice']
    -- , p_allow_admin => true               -- only for SUPER_ADMIN accounts
);

ROLLBACK;   -- <-- replace with COMMIT; when the dry run looks right


-- =============================================================================
-- 6. SAFER ALTERNATIVE: ANONYMISE INSTEAD OF DELETE
--    Keeps orders, invoices and ledgers consistent (financial records usually
--    have to be retained by law), removes personal data, blocks login. This is
--    what the app's DataDeletionService does, minus the NULL date_of_birth
--    (the column is NOT NULL).
-- =============================================================================
-- BEGIN;
-- UPDATE manacommunity.app_user
-- SET    full_name       = 'Deactivated User',
--        email           = 'anon_' || id || '@deleted.manacommunity.com',
--        phone           = '0' || lpad(id::text, 13, '0'),      -- unique, <= 15 chars
--        password_hash   = '!',                                  -- can never match a hash
--        date_of_birth   = DATE '1900-01-01',
--        profile_pic_url = NULL,
--        govt_id_type    = NULL,
--        govt_id_number  = NULL,
--        flat_no         = NULL,
--        block           = NULL,
--        tower           = NULL,
--        employee_id     = NULL,
--        notify_email = false, notify_sms = false, notify_whatsapp = false, notify_push = false,
--        is_active       = false,
--        updated_at      = now()
-- WHERE  id = 12345;
-- DELETE FROM manacommunity.app_user_aud           WHERE id      = 12345;  -- Envers history still holds the old PII
-- DELETE FROM manacommunity.user_privacy_settings  WHERE user_id = 12345;
-- COMMIT;
