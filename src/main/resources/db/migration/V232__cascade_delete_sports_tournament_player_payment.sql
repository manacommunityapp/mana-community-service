-- ──────────────────────────────────────────────────────────────────────────────
-- V232: Ensure ON DELETE CASCADE on sports_tournament_player_payment(tournament_id)
--   Drops Hibernate-generated constraint (fkl32o6rhwelxr2qa5ikqtmkb81) or any default
--   FK on tournament_id and enforces ON DELETE CASCADE so deleting a tournament
--   cascades cleanly to all associated player payments without FK violations.
-- ──────────────────────────────────────────────────────────────────────────────

DO $$
DECLARE
    r RECORD;
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.tables 
        WHERE table_name = 'sports_tournament_player_payment'
    ) THEN
        -- Find and drop all existing foreign key constraints on tournament_id
        FOR r IN (
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
            WHERE t.relname = 'sports_tournament_player_payment'
              AND c.contype = 'f'
              AND a.attname = 'tournament_id'
        ) LOOP
            EXECUTE 'ALTER TABLE sports_tournament_player_payment DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
        END LOOP;

        -- Also explicitly drop by exact name if it exists
        EXECUTE 'ALTER TABLE sports_tournament_player_payment DROP CONSTRAINT IF EXISTS fkl32o6rhwelxr2qa5ikqtmkb81';
        EXECUTE 'ALTER TABLE sports_tournament_player_payment DROP CONSTRAINT IF EXISTS fk_tourn_payment_tournament';

        -- Re-add foreign key with ON DELETE CASCADE
        EXECUTE 'ALTER TABLE sports_tournament_player_payment ADD CONSTRAINT fk_tourn_payment_tournament FOREIGN KEY (tournament_id) REFERENCES sports_tournament(id) ON DELETE CASCADE';
    END IF;
END $$;
