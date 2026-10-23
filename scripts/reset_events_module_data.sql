-- ============================================================================
-- MANA COMMUNITY SERVICE - EVENTS MODULE DATA RESET QUERIES (POSTGRESQL)
-- Schema: manacommunity
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. CLEAR ANY ABORTED TRANSACTION STATE
-- ----------------------------------------------------------------------------
ROLLBACK;


-- ============================================================================
-- OPTION 1: COMPLETE CLEAN WIPE (ALL EVENTS, SCHEDULES & REGISTRATIONS)
-- Deletes all parent events, sub-events, poojas, meals, media, sponsors,
-- expenses, tasks, and registrations. Resets all sequence IDs back to 1.
-- ============================================================================
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


-- ============================================================================
-- OPTION 2: CLEAR USER-ENTERED BOOKINGS & PASSES ONLY (KEEPS MASTER EVENTS)
-- Deletes devotee registrations, passes, donations, auction bids, family members,
-- but preserves all configured events, sevas, meals, competitions & programs.
-- ============================================================================
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


-- ============================================================================
-- OPTION 3: SAFE DYNAMIC PL/PGSQL SCRIPT (EXISTS-CHECK FOR ALL TABLES)
-- Checks if each table exists in the 'manacommunity' schema before truncating,
-- avoiding errors if any optional table is missing.
-- ============================================================================
DO $$
DECLARE
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
