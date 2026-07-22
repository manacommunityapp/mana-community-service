package com.manacommunity.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs DDL patches before Hibernate initialises, fixing check constraints
 * that Hibernate's ddl-auto:update won't touch on existing tables.
 */
@Configuration
public class SchemaConstraintPatcher {

    @Component
    public static class ConstraintPatcherDependencyRegistrar implements BeanFactoryPostProcessor {
        private static final Logger log = LoggerFactory.getLogger(ConstraintPatcherDependencyRegistrar.class);

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            try {
                String[] beanNames = beanFactory.getBeanNamesForType(
                        jakarta.persistence.EntityManagerFactory.class, true, false);
                for (String beanName : beanNames) {
                    BeanDefinition def = beanFactory.getBeanDefinition(beanName);
                    String[] existing = def.getDependsOn();
                    List<String> deps = existing == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(existing));
                    if (!deps.contains("constraintPatcher")) {
                        deps.add("constraintPatcher");
                        def.setDependsOn(deps.toArray(new String[0]));
                    }
                }
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher dependency registration failed: {}", e.getMessage());
            }
        }
    }

    @Component("constraintPatcher")
    public static class ConstraintPatcher {
        private static final Logger log = LoggerFactory.getLogger(ConstraintPatcher.class);

        @Autowired
        private DataSource dataSource;

        @PostConstruct
        public void patch() {
            try (Connection conn = dataSource.getConnection()) {
                String jdbcUrl = conn.getMetaData().getURL().toLowerCase();
                if (jdbcUrl.contains("h2") || jdbcUrl.contains(":mem:")) {
                    log.info("H2 database detected; skipping PostgreSQL constraint patching.");
                    return;
                }
            } catch (Exception e) {
                log.warn("Could not determine DB type; skipping constraint patch: {}", e.getMessage());
                return;
            }
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Drop both possible names — original migration used chk_tournament_type,
                // Hibernate ddl-auto:update generates tournament_config_tournament_type_check.
                stmt.execute("""
                        ALTER TABLE manacommunity.tournament_config
                            DROP CONSTRAINT IF EXISTS chk_tournament_type,
                            DROP CONSTRAINT IF EXISTS tournament_config_tournament_type_check
                        """);

                stmt.execute("""
                        ALTER TABLE manacommunity.tournament_config
                            ADD CONSTRAINT chk_tournament_type CHECK (tournament_type IN (
                                'KNOCKOUT','GROUP_KNOCKOUT','ROUND_ROBIN',
                                'DOUBLE_ELIMINATION','SWISS','SUPER_LEAGUE',
                                'KNOCKOUT_SINGLE','KNOCKOUT_DOUBLE',
                                'GROUP_PLAYOFF','LEAGUE','CUSTOM'
                            ))
                        """);

                log.info("tournament_config tournament_type constraint patched with extended values.");

                // Drop and recreate tournament_match status constraint to include DRAFT and BYE
                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.tournament_match') IS NOT NULL THEN
                            ALTER TABLE manacommunity.tournament_match
                                DROP CONSTRAINT IF EXISTS tournament_match_status_check;
                                
                            ALTER TABLE manacommunity.tournament_match
                                ADD CONSTRAINT tournament_match_status_check CHECK (status IN (
                                    'DRAFT','PUBLISHED','SCHEDULED','LIVE','COMPLETED','POSTPONED','CANCELLED','BYE','AUTO_ADVANCED'
                                ));
                          END IF;
                        END $$;
                        """);
                log.info("tournament_match status constraint patched to support DRAFT.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher failed: {}", e.getMessage(), e);
            }

            // Independent patch: ensure tournament_match.config_id has ON DELETE CASCADE
            // so deleting/clearing a config removes its matches in one DB operation.
            // Idempotent — does nothing once the cascade FK is already in place.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          existing_fk text;
                          col_attnum  smallint;
                        BEGIN
                          -- Skip on fresh databases where the table isn't created yet.
                          IF to_regclass('manacommunity.tournament_match') IS NULL THEN
                            RETURN;
                          END IF;

                          -- Already cascading? Nothing to do.
                          IF EXISTS (
                            SELECT 1 FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = 'tournament_match'
                              AND con.conname = 'fk_tournament_match_config'
                              AND con.contype = 'f'
                              AND con.confdeltype = 'c'
                          ) THEN
                            RETURN;
                          END IF;

                          SELECT a.attnum INTO col_attnum
                          FROM pg_attribute a
                          JOIN pg_class rel ON rel.oid = a.attrelid
                          JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                          WHERE nsp.nspname = 'manacommunity'
                            AND rel.relname = 'tournament_match'
                            AND a.attname = 'config_id';

                          -- Drop any existing FK defined on config_id (e.g. a non-cascading one).
                          FOR existing_fk IN
                            SELECT con.conname
                            FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = 'tournament_match'
                              AND con.contype = 'f'
                              AND con.conkey = ARRAY[col_attnum]
                          LOOP
                            EXECUTE format('ALTER TABLE manacommunity.tournament_match DROP CONSTRAINT %I', existing_fk);
                          END LOOP;

                          ALTER TABLE manacommunity.tournament_match
                            ADD CONSTRAINT fk_tournament_match_config
                            FOREIGN KEY (config_id)
                            REFERENCES manacommunity.tournament_config(id)
                            ON DELETE CASCADE;
                        END $$;
                        """);

                log.info("tournament_match.config_id FK patched with ON DELETE CASCADE.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher cascade-FK patch failed: {}", e.getMessage(), e);
            }

            // Independent patch: add secondary indexes on foreign-key columns.
            // PostgreSQL does NOT auto-index FK columns, so joins, filters and
            // cascade-delete checks do full table scans without these. Idempotent
            // (CREATE INDEX IF NOT EXISTS) and column-existence guarded, so any
            // listed (table, column) that doesn't exist is simply skipped.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          fk record;
                        BEGIN
                          FOR fk IN
                            SELECT * FROM (VALUES
                              ('tournament_config','sport_id'),
                              ('tournament_config','community_id'),
                              ('tournament_config','event_id'),
                              ('tournament_config','venue_id'),
                              ('tournament_config','created_by'),
                              ('tournament_match','config_id'),
                              ('tournament_match','group_id'),
                              ('tournament_match','team_a_id'),
                              ('tournament_match','team_b_id'),
                              ('tournament_match','venue_id'),
                              ('tournament_match','court_id'),
                              ('tournament_match','winner_team_id'),
                              ('tournament_group','config_id'),
                              ('group_team_standing','group_id'),
                              ('group_team_standing','team_id'),
                              ('sports_event','sport_id'),
                              ('sports_event','community_id'),
                              ('sports_event','venue_id'),
                              ('sports_event','event_id'),
                              ('sports_event','category_id'),
                              ('sports_event','created_by'),
                              ('sports_event','tournament_id'),
                              ('event_registration','event_id'),
                              ('event_registration','user_id'),
                              ('event_registration','category_id'),
                              ('event_registration','partner_user_id'),
                              ('sports_event_registration','event_id'),
                              ('sports_event_registration','user_id'),
                              ('sports_event_registration','category_id'),
                              ('sports_event_registration','partner_user_id'),
                              ('app_user','role_id'),
                              ('app_user','community_id'),
                              ('roles','community_id'),
                              ('court','venue_id'),
                              ('event_sponsor','event_id'),
                              ('sports_event_sponsor','event_id'),
                              ('sports_event_sponsor','tournament_id'),
                              ('auction_team','config_id'),
                              ('auction_team','owner_user_id'),
                              ('auction_team','captain_user_id'),
                              ('auction_player','config_id'),
                              ('auction_player','user_id'),
                              ('auction_player','assigned_team_id'),
                              ('auction_bid','config_id'),
                              ('auction_bid','player_id'),
                              ('auction_bid','team_id'),
                              ('auction_bid','bid_by_user_id'),
                              ('auction_session_log','config_id'),
                              ('auction_session_log','player_id'),
                              ('auction_session_log','team_id'),
                              ('auction_session_log','performed_by_user_id')
                            ) AS t(tbl, col)
                          LOOP
                            IF EXISTS (
                              SELECT 1 FROM information_schema.columns
                              WHERE table_schema = 'manacommunity'
                                AND table_name = fk.tbl
                                AND column_name = fk.col
                            ) THEN
                              EXECUTE format(
                                'CREATE INDEX IF NOT EXISTS %I ON manacommunity.%I (%I)',
                                'idx_' || fk.tbl || '_' || fk.col, fk.tbl, fk.col);
                            END IF;
                          END LOOP;
                        END $$;
                        """);

                log.info("Foreign-key column indexes ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher FK-index patch failed: {}", e.getMessage(), e);
            }

            // Independent patch: enforce roles.community_id -> community(id) as a real FK.
            // Added NOT VALID so it enforces new/updated rows without failing on any
            // pre-existing orphan data; idempotent and guarded on table existence.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.roles') IS NOT NULL
                             AND to_regclass('manacommunity.community') IS NOT NULL
                             AND NOT EXISTS (
                               SELECT 1 FROM pg_constraint con
                               JOIN pg_class rel ON rel.oid = con.conrelid
                               JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                               WHERE nsp.nspname = 'manacommunity'
                                 AND rel.relname = 'roles'
                                 AND con.conname = 'fk_roles_community'
                                 AND con.contype = 'f'
                             ) THEN
                            ALTER TABLE manacommunity.roles
                              ADD CONSTRAINT fk_roles_community
                              FOREIGN KEY (community_id)
                              REFERENCES manacommunity.community(id)
                              NOT VALID;
                          END IF;
                        END $$;
                        """);

                // Validate the FK against existing rows once data is clean. If orphan
                // community_id values exist, validation fails gracefully (NOTICE) and the
                // FK stays NOT VALID — still enforcing new/updated rows. Idempotent: skips
                // once the constraint is already validated.
                stmt.execute("""
                        DO $$
                        DECLARE
                          is_valid boolean;
                        BEGIN
                          SELECT con.convalidated INTO is_valid
                          FROM pg_constraint con
                          JOIN pg_class rel ON rel.oid = con.conrelid
                          JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                          WHERE nsp.nspname = 'manacommunity'
                            AND rel.relname = 'roles'
                            AND con.conname = 'fk_roles_community'
                            AND con.contype = 'f';

                          IF is_valid IS NOT NULL AND is_valid = false THEN
                            BEGIN
                              ALTER TABLE manacommunity.roles VALIDATE CONSTRAINT fk_roles_community;
                            EXCEPTION WHEN others THEN
                              RAISE NOTICE 'fk_roles_community left NOT VALID (orphan roles.community_id rows?): %', SQLERRM;
                            END;
                          END IF;
                        END $$;
                        """);

                log.info("roles.community_id FK to community ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher roles-community FK patch failed: {}", e.getMessage(), e);
            }

            // One-time data migration (idempotent): copy legacy event_sponsor rows into
            // the consolidated sports_event_sponsor table (tournament_id null). Guarded on
            // the legacy table still existing (skipped on fresh DBs) and on NOT EXISTS so
            // it never duplicates. The legacy event_sponsor rows are left untouched.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.event_sponsor') IS NOT NULL
                             AND to_regclass('manacommunity.sports_event_sponsor') IS NOT NULL THEN
                            INSERT INTO manacommunity.sports_event_sponsor
                              (event_id, tournament_id, category, name, url, created_at)
                            SELECT es.event_id, NULL, es.category, es.name, es.url, es.created_at
                            FROM manacommunity.event_sponsor es
                            WHERE NOT EXISTS (
                              SELECT 1 FROM manacommunity.sports_event_sponsor s
                              WHERE s.event_id = es.event_id
                                AND s.name = es.name
                                AND s.category = es.category);
                          END IF;
                        END $$;
                        """);

                log.info("event_sponsor -> sports_event_sponsor backfill ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher sponsor-backfill failed: {}", e.getMessage(), e);
            }

            // One-time data migration (idempotent): copy PENDING (sent=false) legacy
            // event_notification_schedule rows into sports_notification_scheduler so they
            // still fire through the single remaining loop. Already-sent rows are skipped
            // so nothing is re-sent. Guarded on the legacy table existing + NOT EXISTS.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.event_notification_schedule') IS NOT NULL
                             AND to_regclass('manacommunity.sports_notification_scheduler') IS NOT NULL THEN
                            INSERT INTO manacommunity.sports_notification_scheduler
                              (event_id, tournament_id, trigger_key, label, offset_minutes, enabled,
                               title, body, recipients, channels, priority, is_custom, sent, notify_at, created_at)
                            SELECT e.event_id, NULL, e.type, COALESCE(e.type,'Reminder'), 0, true,
                                   COALESCE(e.title,'Reminder'), COALESCE(e.body,''),
                                   'Registered Players', 'push,email', 'NORMAL', true,
                                   false, e.notify_at, e.created_at
                            FROM manacommunity.event_notification_schedule e
                            WHERE e.sent = false
                              AND NOT EXISTS (
                                SELECT 1 FROM manacommunity.sports_notification_scheduler s
                                WHERE s.event_id = e.event_id
                                  AND s.notify_at = e.notify_at
                                  AND s.title = COALESCE(e.title,'Reminder'));
                          END IF;
                        END $$;
                        """);

                log.info("event_notification_schedule -> sports_notification_scheduler backfill ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher notification-backfill failed: {}", e.getMessage(), e);
            }

            // Ensure the sports_event_registration.email column exists before Hibernate
            // validates (prod runs ddl-auto=validate). Idempotent + table-guarded.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.sports_event_registration') IS NOT NULL THEN
                            ALTER TABLE manacommunity.sports_event_registration
                              ADD COLUMN IF NOT EXISTS email varchar(255);
                          END IF;
                        END $$;
                        """);

                log.info("sports_event_registration.email column ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher email-column patch failed: {}", e.getMessage(), e);
            }

            // Ensure the venue.contact_title column exists before Hibernate validates.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.venue') IS NOT NULL THEN
                            ALTER TABLE manacommunity.venue
                              ADD COLUMN IF NOT EXISTS contact_title varchar(100);
                          END IF;
                        END $$;
                        """);

                log.info("venue.contact_title column ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher venue contact_title column patch failed: {}", e.getMessage(), e);
            }

            // Ensure the app_user brute-force lockout columns exist before Hibernate
            // validates (prod runs ddl-auto=validate). Idempotent + table-guarded.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.app_user') IS NOT NULL THEN
                            ALTER TABLE manacommunity.app_user
                              ADD COLUMN IF NOT EXISTS failed_login_attempts integer NOT NULL DEFAULT 0;
                            ALTER TABLE manacommunity.app_user
                              ADD COLUMN IF NOT EXISTS locked_until timestamp;
                          END IF;
                        END $$;
                        """);

                log.info("app_user lockout columns (failed_login_attempts, locked_until) ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher lockout-column patch failed: {}", e.getMessage(), e);
            }

            // Ensure sports_event.uuid (public link id) and admin_approval_required exist
            // before Hibernate validates (prod runs ddl-auto=validate). Backfills any
            // pre-existing rows with a fresh UUID, then adds the unique index the entity
            // declares. Requires pgcrypto's gen_random_uuid(); falls back gracefully.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.sports_event') IS NOT NULL THEN
                            CREATE EXTENSION IF NOT EXISTS pgcrypto;

                            ALTER TABLE manacommunity.sports_event
                              ADD COLUMN IF NOT EXISTS uuid uuid;
                            ALTER TABLE manacommunity.sports_event
                              ADD COLUMN IF NOT EXISTS admin_approval_required boolean NOT NULL DEFAULT true;

                            UPDATE manacommunity.sports_event
                              SET uuid = gen_random_uuid()
                              WHERE uuid IS NULL;

                            CREATE UNIQUE INDEX IF NOT EXISTS idx_sports_event_uuid
                              ON manacommunity.sports_event (uuid);
                          END IF;
                        END $$;
                        """);

                log.info("sports_event uuid + admin_approval_required columns ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher sports_event uuid/approval patch failed: {}", e.getMessage(), e);
            }

            // Ensure the email_otp table exists before Hibernate validates (prod runs
            // ddl-auto=validate, which won't create new entity tables). Idempotent.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_otp (
                            id          BIGSERIAL PRIMARY KEY,
                            email       VARCHAR(255) NOT NULL,
                            code_hash   VARCHAR(100) NOT NULL,
                            expires_at  TIMESTAMP NOT NULL,
                            attempts    INTEGER NOT NULL DEFAULT 0,
                            consumed    BOOLEAN NOT NULL DEFAULT false,
                            verified    BOOLEAN NOT NULL DEFAULT false,
                            verified_at TIMESTAMP,
                            created_at  TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_otp_email ON manacommunity.email_otp (email)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_otp_created_at ON manacommunity.email_otp (created_at)");

                log.info("email_otp table ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher email_otp table patch failed: {}", e.getMessage(), e);
            }

            // Ensure the chat tables exist before Hibernate validates (prod runs
            // ddl-auto=validate, which won't create new entity tables). Idempotent.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.chat_conversation (
                            id              BIGSERIAL PRIMARY KEY,
                            type            VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
                            title           VARCHAR(120),
                            community_id    BIGINT,
                            last_message    TEXT,
                            last_message_at TIMESTAMP,
                            created_at      TIMESTAMP NOT NULL DEFAULT now(),
                            updated_at      TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.chat_participant (
                            id              BIGSERIAL PRIMARY KEY,
                            conversation_id BIGINT NOT NULL,
                            user_id         BIGINT NOT NULL,
                            last_read_at    TIMESTAMP,
                            created_at      TIMESTAMP NOT NULL DEFAULT now(),
                            CONSTRAINT uk_chat_participant_conv_user UNIQUE (conversation_id, user_id)
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.chat_message (
                            id              BIGSERIAL PRIMARY KEY,
                            conversation_id BIGINT NOT NULL,
                            sender_id       BIGINT,
                            type            VARCHAR(20) NOT NULL DEFAULT 'TEXT',
                            content         TEXT NOT NULL,
                            created_at      TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_participant_user ON manacommunity.chat_participant (user_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_participant_conv ON manacommunity.chat_participant (conversation_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_message_conv ON manacommunity.chat_message (conversation_id)");

                log.info("chat tables ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher chat table patch failed: {}", e.getMessage(), e);
            }

            // Ensure the chat child-table FKs to chat_conversation cascade on delete,
            // so removing a conversation also removes its messages + participants in
            // one DB operation (Hibernate's generated FKs are NO ACTION). Idempotent:
            // finds the existing FK on each column by name, drops it, and re-adds a
            // named cascading FK; skips once our cascading FK is already in place.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          rec         record;
                          existing_fk text;
                          col_attnum  smallint;
                        BEGIN
                          FOR rec IN
                            SELECT * FROM (VALUES
                              ('chat_message','conversation_id','fk_chat_message_conversation'),
                              ('chat_participant','conversation_id','fk_chat_participant_conversation')
                            ) AS t(child, col, fkname)
                          LOOP
                            -- Skip on fresh databases where the table isn't created yet.
                            CONTINUE WHEN to_regclass('manacommunity.' || rec.child) IS NULL;

                            -- Already cascading under our name? Nothing to do.
                            IF EXISTS (
                              SELECT 1 FROM pg_constraint con
                              JOIN pg_class rel ON rel.oid = con.conrelid
                              JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                              WHERE nsp.nspname = 'manacommunity'
                                AND rel.relname = rec.child
                                AND con.conname = rec.fkname
                                AND con.contype = 'f'
                                AND con.confdeltype = 'c'
                            ) THEN
                              CONTINUE;
                            END IF;

                            SELECT a.attnum INTO col_attnum
                            FROM pg_attribute a
                            JOIN pg_class rel ON rel.oid = a.attrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = rec.child
                              AND a.attname = rec.col;

                            -- Drop any existing FK defined on that column (non-cascading one).
                            FOR existing_fk IN
                              SELECT con.conname
                              FROM pg_constraint con
                              JOIN pg_class rel ON rel.oid = con.conrelid
                              JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                              WHERE nsp.nspname = 'manacommunity'
                                AND rel.relname = rec.child
                                AND con.contype = 'f'
                                AND con.conkey = ARRAY[col_attnum]
                            LOOP
                              EXECUTE format('ALTER TABLE manacommunity.%I DROP CONSTRAINT %I', rec.child, existing_fk);
                            END LOOP;

                            EXECUTE format(
                              'ALTER TABLE manacommunity.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '
                              || 'REFERENCES manacommunity.chat_conversation(id) ON DELETE CASCADE',
                              rec.child, rec.fkname, rec.col);
                          END LOOP;
                        END $$;
                        """);

                log.info("chat child-table FKs patched with ON DELETE CASCADE.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher chat cascade-FK patch failed: {}", e.getMessage(), e);
            }

            // Widen banner_image columns from the default varchar(255) to TEXT.
            // They store either a URL or an inline base64 data-URI; base64 images
            // overflow varchar(255) and Postgres raises SQLSTATE 22001
            // ("value too long"), surfaced to the client as a 409. Prod runs
            // ddl-auto=validate so Hibernate won't alter the live column — do it
            // here. Guarded on the column still being character varying so the
            // (potentially large) column is never rewritten on later boots.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          rec record;
                        BEGIN
                          FOR rec IN
                            SELECT * FROM (VALUES
                              ('tournament','banner_image'),
                              ('sports_event','banner_image')
                            ) AS t(tbl, col)
                          LOOP
                            IF EXISTS (
                              SELECT 1 FROM information_schema.columns
                              WHERE table_schema = 'manacommunity'
                                AND table_name = rec.tbl
                                AND column_name = rec.col
                                AND data_type = 'character varying'
                            ) THEN
                              EXECUTE format(
                                'ALTER TABLE manacommunity.%I ALTER COLUMN %I TYPE TEXT',
                                rec.tbl, rec.col);
                            END IF;
                          END LOOP;
                        END $$;
                        """);

                log.info("banner_image columns widened to TEXT.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher banner_image widen patch failed: {}", e.getMessage(), e);
            }

            // Make sports_event.tournament_id -> tournament(id) SET NULL on delete,
            // so deleting a tournament never gets blocked by its events: the events
            // survive with tournament_id nulled rather than the FK rejecting the
            // delete. Hibernate generates this FK as NO ACTION; prod runs
            // ddl-auto=validate so it won't change it — do it here. Idempotent:
            // finds the existing FK on the column, drops it, and re-adds a named
            // FK with ON DELETE SET NULL; skips once ours is already in place.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          existing_fk text;
                          col_attnum  smallint;
                        BEGIN
                          -- Skip on fresh databases where the table isn't created yet.
                          IF to_regclass('manacommunity.sports_event') IS NULL THEN
                            RETURN;
                          END IF;

                          -- Already SET NULL under our name? Nothing to do.
                          IF EXISTS (
                            SELECT 1 FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = 'sports_event'
                              AND con.conname = 'fk_sports_event_tournament'
                              AND con.contype = 'f'
                              AND con.confdeltype = 'n'
                          ) THEN
                            RETURN;
                          END IF;

                          SELECT a.attnum INTO col_attnum
                          FROM pg_attribute a
                          JOIN pg_class rel ON rel.oid = a.attrelid
                          JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                          WHERE nsp.nspname = 'manacommunity'
                            AND rel.relname = 'sports_event'
                            AND a.attname = 'tournament_id';

                          -- Drop any existing FK defined on tournament_id (e.g. NO ACTION).
                          FOR existing_fk IN
                            SELECT con.conname
                            FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                            WHERE nsp.nspname = 'manacommunity'
                              AND rel.relname = 'sports_event'
                              AND con.contype = 'f'
                              AND con.conkey = ARRAY[col_attnum]
                          LOOP
                            EXECUTE format('ALTER TABLE manacommunity.sports_event DROP CONSTRAINT %I', existing_fk);
                          END LOOP;

                          ALTER TABLE manacommunity.sports_event
                            ADD CONSTRAINT fk_sports_event_tournament
                            FOREIGN KEY (tournament_id)
                            REFERENCES manacommunity.tournament(id)
                            ON DELETE SET NULL;
                        END $$;
                        """);

                log.info("sports_event.tournament_id FK patched with ON DELETE SET NULL.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher sports_event tournament FK patch failed: {}", e.getMessage(), e);
            }

            // Ensure the app_user columns captured by the admin create-user form
            // exist and that profile_pic_url can hold a base64 data-URI. Prod runs
            // ddl-auto=validate so Hibernate won't add/alter these. Idempotent:
            // ADD COLUMN IF NOT EXISTS, and only widen profile_pic_url while it's
            // still varchar so the column isn't rewritten on later boots.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        BEGIN
                          IF to_regclass('manacommunity.app_user') IS NOT NULL THEN
                            IF EXISTS (
                              SELECT 1 FROM information_schema.columns
                              WHERE table_schema = 'manacommunity'
                                AND table_name = 'app_user'
                                AND column_name = 'profile_pic_url'
                                AND data_type = 'character varying'
                            ) THEN
                              ALTER TABLE manacommunity.app_user
                                ALTER COLUMN profile_pic_url TYPE TEXT;
                            END IF;

                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS employee_id       varchar(50);
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS tower             varchar(30);
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS resident_type     varchar(30);
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS occupancy_status  varchar(30);
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS notify_email      boolean DEFAULT true;
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS notify_sms        boolean DEFAULT false;
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS notify_whatsapp   boolean DEFAULT true;
                            ALTER TABLE manacommunity.app_user ADD COLUMN IF NOT EXISTS notify_push       boolean DEFAULT true;
                          END IF;
                        END $$;
                        """);

                log.info("app_user create-user columns ensured (profile_pic_url TEXT + profile/notification fields).");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher app_user create-user columns patch failed: {}", e.getMessage(), e);
            }

            // Make deleting a SportsEvent ("tournament" in the sports UI) clean up its
            // whole event-scoped subtree instead of failing on non-cascading FKs
            // (registrations, scheduling config -> matches/groups/standings, auction
            // config -> teams/players/bids/logs). Hibernate generates these FKs as
            // NO ACTION and prod runs ddl-auto=validate, so set the behaviour here.
            //
            // action 'c' = ON DELETE CASCADE (ownership: the child is meaningless
            //   without its parent and is removed with it).
            // action 'n' = ON DELETE SET NULL (cross-reference: the referencing row
            //   should survive a standalone delete of the pointed-to row; the column
            //   is nullable). During a full event teardown these rows are removed via
            //   their own ownership cascade, and SET NULL only avoids a transient
            //   ordering violation.
            // Idempotent: skips a column whose FK already has the desired action.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DO $$
                        DECLARE
                          rec         record;
                          existing_fk text;
                          col_attnum  smallint;
                          want        char;
                        BEGIN
                          FOR rec IN
                            SELECT * FROM (VALUES
                              -- (child_table, fk_column, parent_table, action)
                              ('sports_event_registration','event_id','sports_event','c'),
                              ('tournament_config','event_id','sports_event','c'),
                              ('auction_config','event_id','sports_event','c'),
                              ('sports_event_sponsor','event_id','sports_event','c'),
                              ('sports_notification_scheduler','event_id','sports_event','c'),
                              ('tournament_group','config_id','tournament_config','c'),
                              ('tournament_match','config_id','tournament_config','c'),
                              ('group_team_standing','group_id','tournament_group','c'),
                              ('auction_team','config_id','auction_config','c'),
                              ('auction_player','config_id','auction_config','c'),
                              ('auction_bid','config_id','auction_config','c'),
                              ('auction_session_log','config_id','auction_config','c'),
                              ('auction_dispute_committee','config_id','auction_config','c'),
                              ('auction_bid','team_id','auction_team','c'),
                              ('auction_bid','player_id','auction_player','c'),
                              ('auction_session_log','team_id','auction_team','c'),
                              ('auction_session_log','player_id','auction_player','c'),
                              ('group_team_standing','team_id','auction_team','c'),
                              ('auction_player','assigned_team_id','auction_team','n'),
                              ('tournament_match','group_id','tournament_group','n'),
                              ('tournament_match','team_a_id','auction_team','n'),
                              ('tournament_match','team_b_id','auction_team','n'),
                              ('tournament_match','winner_team_id','auction_team','n'),
                              ('tournament_match','toss_winner_team_id','auction_team','n'),
                              ('tournament_match','man_of_match_id','auction_player','n'),
                              -- Venue delete: courts belong to the venue (removed with
                              -- it); events/matches survive with a null (TBD) venue.
                              ('court','venue_id','venue','c'),
                              ('sports_event','venue_id','venue','n'),
                              ('tournament_match','venue_id','venue','n'),
                              -- Player-category delete: unlink from events (join rows)
                              -- and null the category on registrations, which survive.
                              ('event_category','category_id','player_category','c'),
                              ('event_category','event_id','sports_event','c'),
                              ('sports_event_registration','category_id','player_category','n')
                            ) AS t(child, col, parent, act)
                          LOOP
                            CONTINUE WHEN to_regclass('manacommunity.' || rec.child) IS NULL;
                            CONTINUE WHEN to_regclass('manacommunity.' || rec.parent) IS NULL;
                            CONTINUE WHEN NOT EXISTS (
                              SELECT 1 FROM information_schema.columns
                              WHERE table_schema = 'manacommunity'
                                AND table_name = rec.child AND column_name = rec.col);

                            want := CASE WHEN rec.act = 'c' THEN 'c' ELSE 'n' END;

                            SELECT a.attnum INTO col_attnum
                            FROM pg_attribute a
                            WHERE a.attrelid = ('manacommunity.' || rec.child)::regclass
                              AND a.attname = rec.col;

                            -- Already the desired action on a FK over this column? Skip.
                            IF EXISTS (
                              SELECT 1 FROM pg_constraint con
                              JOIN pg_class rel ON rel.oid = con.conrelid
                              JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                              WHERE nsp.nspname = 'manacommunity'
                                AND rel.relname = rec.child
                                AND con.contype = 'f'
                                AND con.conkey = ARRAY[col_attnum]
                                AND con.confdeltype = want
                            ) THEN
                              CONTINUE;
                            END IF;

                            -- Drop any existing FK on this column, then re-add with the action.
                            FOR existing_fk IN
                              SELECT con.conname FROM pg_constraint con
                              JOIN pg_class rel ON rel.oid = con.conrelid
                              JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                              WHERE nsp.nspname = 'manacommunity'
                                AND rel.relname = rec.child
                                AND con.contype = 'f'
                                AND con.conkey = ARRAY[col_attnum]
                            LOOP
                              EXECUTE format('ALTER TABLE manacommunity.%I DROP CONSTRAINT %I', rec.child, existing_fk);
                            END LOOP;

                            EXECUTE format(
                              'ALTER TABLE manacommunity.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '
                              || 'REFERENCES manacommunity.%I(id) ON DELETE %s',
                              rec.child, 'fk_' || rec.child || '_' || rec.col, rec.col, rec.parent,
                              CASE WHEN want = 'c' THEN 'CASCADE' ELSE 'SET NULL' END);
                          END LOOP;
                        END $$;
                        """);

                log.info("SportsEvent delete subtree FKs patched (cascade/set-null for registrations, scheduling and auction graphs).");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher event-delete-subtree FK patch failed: {}", e.getMessage(), e);
            }

            // Tournament announcement content tables (announcements, gallery,
            // timeline) that back the announcement email. Prod runs
            // ddl-auto=validate, which won't create new entity tables. Idempotent;
            // FKs cascade so a tournament's content is removed with it.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.tournament_announcement (
                            id            BIGSERIAL PRIMARY KEY,
                            tournament_id BIGINT NOT NULL REFERENCES manacommunity.tournament(id) ON DELETE CASCADE,
                            title         VARCHAR(150),
                            content       VARCHAR(2000) NOT NULL,
                            icon          VARCHAR(20),
                            sort_order    INTEGER DEFAULT 0,
                            created_at    TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tournament_announcement_tid ON manacommunity.tournament_announcement (tournament_id)");

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.tournament_gallery_image (
                            id            BIGSERIAL PRIMARY KEY,
                            tournament_id BIGINT NOT NULL REFERENCES manacommunity.tournament(id) ON DELETE CASCADE,
                            title         VARCHAR(100),
                            image_url     TEXT,
                            bg_color      VARCHAR(20),
                            icon          VARCHAR(20),
                            sort_order    INTEGER DEFAULT 0,
                            created_at    TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tournament_gallery_tid ON manacommunity.tournament_gallery_image (tournament_id)");

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.tournament_timeline_entry (
                            id            BIGSERIAL PRIMARY KEY,
                            tournament_id BIGINT NOT NULL REFERENCES manacommunity.tournament(id) ON DELETE CASCADE,
                            entry_date    DATE,
                            date_label    VARCHAR(60),
                            title         VARCHAR(150) NOT NULL,
                            description   VARCHAR(500),
                            sort_order    INTEGER DEFAULT 0,
                            created_at    TIMESTAMP NOT NULL DEFAULT now()
                        )
                        """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tournament_timeline_tid ON manacommunity.tournament_timeline_entry (tournament_id)");

                log.info("Tournament content tables ensured (announcement, gallery, timeline).");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher tournament content tables patch failed: {}", e.getMessage(), e);
            }

            // Multi-tenant email template management (branding, headers, footers,
            // templates, versions, dynamic fields). Prod runs ddl-auto=validate, so
            // the tables must be created here rather than relying on Hibernate.
            // Idempotent: CREATE ... IF NOT EXISTS + ON CONFLICT DO NOTHING seeding,
            // safe to re-run every boot. Mirrors db/sql/v1.0.0/35_email_template_management.sql.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.community_branding (
                            id              BIGSERIAL       PRIMARY KEY,
                            community_id    BIGINT          NOT NULL UNIQUE REFERENCES manacommunity.community(id),
                            logo_url        VARCHAR(500),
                            primary_color   VARCHAR(20)     NOT NULL DEFAULT '#2563eb',
                            secondary_color VARCHAR(20)     DEFAULT '#0f172a',
                            font_family     VARCHAR(120),
                            button_style    VARCHAR(40),
                            banner_url      VARCHAR(500),
                            facebook_url    VARCHAR(500),
                            instagram_url   VARCHAR(500),
                            website_url     VARCHAR(500),
                            support_email   VARCHAR(160),
                            support_phone   VARCHAR(40),
                            created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                            updated_at      TIMESTAMP       NOT NULL DEFAULT now()
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_header (
                            id              BIGSERIAL       PRIMARY KEY,
                            community_id    BIGINT          NOT NULL REFERENCES manacommunity.community(id),
                            name            VARCHAR(120)    NOT NULL,
                            html_content    TEXT            NOT NULL,
                            layout_json     TEXT,
                            is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                            version         INTEGER         NOT NULL DEFAULT 1,
                            created_by      BIGINT,
                            created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                            updated_at      TIMESTAMP       NOT NULL DEFAULT now()
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_footer (
                            id              BIGSERIAL       PRIMARY KEY,
                            community_id    BIGINT          NOT NULL REFERENCES manacommunity.community(id),
                            name            VARCHAR(120)    NOT NULL,
                            html_content    TEXT            NOT NULL,
                            layout_json     TEXT,
                            is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                            version         INTEGER         NOT NULL DEFAULT 1,
                            created_by      BIGINT,
                            created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                            updated_at      TIMESTAMP       NOT NULL DEFAULT now()
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_template (
                            id              BIGSERIAL       PRIMARY KEY,
                            community_id    BIGINT          NOT NULL REFERENCES manacommunity.community(id),
                            template_code   VARCHAR(80)     NOT NULL,
                            template_name   VARCHAR(160)    NOT NULL,
                            category        VARCHAR(80),
                            subject         VARCHAR(240)    NOT NULL,
                            html_content    TEXT            NOT NULL,
                            layout_json     TEXT,
                            header_id       BIGINT          REFERENCES manacommunity.email_header(id),
                            footer_id       BIGINT          REFERENCES manacommunity.email_footer(id),
                            status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
                            version         INTEGER         NOT NULL DEFAULT 1,
                            created_by      BIGINT,
                            created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                            updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
                            CONSTRAINT uk_email_template_community_code UNIQUE (community_id, template_code),
                            CONSTRAINT ck_email_template_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_template_version (
                            id              BIGSERIAL       PRIMARY KEY,
                            template_id     BIGINT          NOT NULL REFERENCES manacommunity.email_template(id) ON DELETE CASCADE,
                            version         INTEGER         NOT NULL,
                            subject         VARCHAR(240)    NOT NULL,
                            html_content    TEXT            NOT NULL,
                            layout_json     TEXT,
                            header_id       BIGINT          REFERENCES manacommunity.email_header(id),
                            footer_id       BIGINT          REFERENCES manacommunity.email_footer(id),
                            status          VARCHAR(20)     NOT NULL,
                            created_by      BIGINT,
                            notes           VARCHAR(500),
                            created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                            CONSTRAINT uk_email_template_version UNIQUE (template_id, version),
                            CONSTRAINT ck_email_template_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_dynamic_field (
                            id              BIGSERIAL       PRIMARY KEY,
                            category        VARCHAR(80)     NOT NULL,
                            field_key       VARCHAR(120)    NOT NULL UNIQUE,
                            display_name    VARCHAR(140)    NOT NULL,
                            sample_value    VARCHAR(500),
                            description     VARCHAR(500),
                            is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                            sort_order      INTEGER         NOT NULL DEFAULT 0
                        )
                        """);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_header_community ON manacommunity.email_header(community_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_footer_community ON manacommunity.email_footer(community_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_template_community_status ON manacommunity.email_template(community_id, status)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_template_code ON manacommunity.email_template(template_code)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_email_header_community_name ON manacommunity.email_header(community_id, name)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_email_footer_community_name ON manacommunity.email_footer(community_id, name)");

                log.info("Email template management tables ensured (default header/footer/templates are seeded via EmailTemplateFeeder, not here).");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher email template management patch failed: {}", e.getMessage(), e);
            }

            // Freeform templates composed in the GrapesJS drag-and-drop builder —
            // separate from the code-keyed email_template table above.
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS manacommunity.email_builder_template (
                            id            BIGSERIAL       PRIMARY KEY,
                            community_id  BIGINT          NOT NULL REFERENCES manacommunity.community(id),
                            name          VARCHAR(160)    NOT NULL,
                            subject       VARCHAR(240)    NOT NULL,
                            html_content  TEXT            NOT NULL,
                            css           TEXT,
                            layout_json   TEXT,
                            status        VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
                            version       INTEGER         NOT NULL DEFAULT 1,
                            created_at    TIMESTAMP       NOT NULL DEFAULT now(),
                            updated_at    TIMESTAMP       NOT NULL DEFAULT now(),
                            CONSTRAINT ck_email_builder_template_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
                        )
                        """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_email_builder_template_community ON manacommunity.email_builder_template(community_id)");

                log.info("Email builder template table ensured.");
            } catch (Exception e) {
                log.error("SchemaConstraintPatcher email builder template patch failed: {}", e.getMessage(), e);
            }
        }
    }
}
