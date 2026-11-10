-- ============================================================================
-- MANA COMMUNITY SERVICE - EVENTS MODULE DATA RESET QUERIES (POSTGRESQL)
-- Schema: manacommunity
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. CLEAR ANY ABORTED TRANSACTION STATE
-- ----------------------------------------------------------------------------
ROLLBACK;

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
        RAISE EXCEPTION 'CRITICAL ENVIRONMENT SAFETY ERROR: Destructive reset scripts are strictly prohibited on remote/AWS servers! Detected database server IP: %. Execution ABORTED.', v_server_addr;
    END IF;

    -- 2. EXPLICIT LOCAL CONFIRMATION GUARD:
    -- Prevent accidental execution. Must run `SET app.allow_local_reset = 'true';` first.
    IF v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'SAFETY GUARD: This script will WIPE events module data and is restricted to LOCAL environments only. To run on your LOCAL database, you must first execute in this session: SET app.allow_local_reset = ''true'';';
    END IF;
END $$;


-- ============================================================================
-- OPTION 1: COMPLETE CLEAN WIPE (ALL EVENTS, SCHEDULES & REGISTRATIONS)
-- Deletes all parent events, sub-events, poojas, meals, media, sponsors,
-- expenses, tasks, and registrations. Resets all sequence IDs back to 1.
-- ============================================================================
DO $$
DECLARE
    v_server_addr inet := inet_server_addr();
    v_allow_reset text := current_setting('app.allow_local_reset', true);
BEGIN
    IF (v_server_addr IS NOT NULL AND v_server_addr NOT IN ('127.0.0.1'::inet, '::1'::inet)) OR v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'CRITICAL BLOCKED: Option 1 TRUNCATE can ONLY run on a LOCAL database (127.0.0.1) with SET app.allow_local_reset = ''true''; (detected server: %)', v_server_addr;
    END IF;

    TRUNCATE TABLE 
        manacommunity.event_booking_registrations,
        manacommunity.event_activity_registrations,
        manacommunity.event_pooja_user_registrations,
        manacommunity.event_registration,
        manacommunity.event_meal_registrations,
        manacommunity.event_family_members,
        manacommunity.event_donation,
        manacommunity.event_auction_bid,
        manacommunity.event_volunteer,
        manacommunity.event_expense,
        manacommunity.event_invoice,
        manacommunity.event_task,
        manacommunity.event_sponsor,
        manacommunity.event_gallery_item,
        manacommunity.event_scheduled_notification,
        manacommunity.event_contacts,
        manacommunity.event_pooja_sevas,
        manacommunity.event_lunch_dinners,
        manacommunity.event_cultural_events,
        manacommunity.event_competitions,
        manacommunity.event_auction_item,
        manacommunity.event_program,
        manacommunity.event_venues,
        manacommunity.event_community
    RESTART IDENTITY CASCADE;

    RAISE NOTICE 'Option 1: Complete clean wipe executed.';
END $$;


-- ============================================================================
-- OPTION 2: CLEAR USER-ENTERED BOOKINGS & PASSES ONLY (KEEPS MASTER EVENTS)
-- Deletes devotee registrations, passes, donations, auction bids, family members,
-- but preserves all configured events, sevas, meals, competitions & programs.
-- ============================================================================
DO $$
DECLARE
    v_server_addr inet := inet_server_addr();
    v_allow_reset text := current_setting('app.allow_local_reset', true);
BEGIN
    IF (v_server_addr IS NOT NULL AND v_server_addr NOT IN ('127.0.0.1'::inet, '::1'::inet)) OR v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'CRITICAL BLOCKED: Option 2 TRUNCATE can ONLY run on a LOCAL database (127.0.0.1) with SET app.allow_local_reset = ''true''; (detected server: %)', v_server_addr;
    END IF;

    TRUNCATE TABLE 
        manacommunity.event_booking_registrations,
        manacommunity.event_activity_registrations,
        manacommunity.event_pooja_user_registrations,
        manacommunity.event_registration,
        manacommunity.event_meal_registrations,
        manacommunity.event_family_members,
        manacommunity.event_donation,
        manacommunity.event_auction_bid,
        manacommunity.event_volunteer
    RESTART IDENTITY CASCADE;

    RAISE NOTICE 'Option 2: Bookings and registrations cleared.';
END $$;


-- ============================================================================
-- OPTION 3: SAFE DYNAMIC PL/PGSQL SCRIPT (EXISTS-CHECK FOR ALL TABLES)
-- Checks if each table exists in the 'manacommunity' schema before truncating,
-- avoiding errors if any optional table is missing.
-- ============================================================================
DO $$
DECLARE
    v_server_addr inet := inet_server_addr();
    v_allow_reset text := current_setting('app.allow_local_reset', true);
    tbl text;
    tables_to_clear text[] := ARRAY[
        'event_booking_registrations',
        'event_activity_registrations',
        'event_pooja_user_registrations',
        'event_registration',
        'event_meal_registrations',
        'event_pooja_slot_reservation',
        'pooja_slot_reservation',
        'event_family_members',
        'event_donation',
        'event_auction_bid',
        'event_volunteer',
        'event_expense',
        'event_invoice',
        'event_task',
        'event_sponsor',
        'event_gallery_item',
        'event_scheduled_notification',
        'event_contacts',
        'event_pooja_sevas',
        'event_pooja_seva_start_times',
        'pooja_schedule',
        'event_lunch_dinners',
        'event_cultural_events',
        'event_competitions',
        'event_auction_item',
        'event_program',
        'event_venues',
        'event_ticket_categories',
        'event_ticket_category_names',
        'event_media_day',
        'event_media_category',
        'event_community'
    ];
BEGIN
    IF (v_server_addr IS NOT NULL AND v_server_addr NOT IN ('127.0.0.1'::inet, '::1'::inet)) OR v_allow_reset IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'CRITICAL BLOCKED: Option 3 TRUNCATE can ONLY run on a LOCAL database (127.0.0.1) with SET app.allow_local_reset = ''true''; (detected server: %)', v_server_addr;
    END IF;

    FOREACH tbl IN ARRAY tables_to_clear
    LOOP
        IF EXISTS (
            SELECT 1 
            FROM information_schema.tables 
            WHERE table_schema = 'manacommunity' AND table_name = tbl
        ) THEN
            EXECUTE 'TRUNCATE TABLE manacommunity.' || quote_ident(tbl) || ' RESTART IDENTITY CASCADE';
            RAISE NOTICE 'Successfully cleared: manacommunity.%', tbl;
        END IF;
    END LOOP;
END $$;
