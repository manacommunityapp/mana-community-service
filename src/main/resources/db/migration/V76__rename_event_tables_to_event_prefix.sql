-- V76: Standardise event-module table names to the event_ prefix.
--
-- PostgreSQL ALTER TABLE RENAME preserves all indexes, constraints,
-- triggers, and foreign-key references automatically — no manual FK
-- recreation is required.
--
-- Tables skipped (already have event_ prefix):
--   event_competitions, event_competition_age_groups, event_competition_categories,
--   event_cultural_events, event_cultural_performance_types, event_cultural_categories,
--   event_auction_bid, event_auction_item, event_booking_registrations,
--   event_donation, event_invoice, event_family_members, event_gallery_item,
--   event_media_category, event_media_day, event_pooja_user_registrations,
--   event_registration, event_program, event_venues, event_ticket_categories,
--   event_ticket_category_names, event_pooja_types, event_pooja_sevas,
--   event_lunch_dinners, event_lunch_dinner_menu_items

-- ── community_event → event_community ───────────────────────────────────────
ALTER TABLE IF EXISTS manacommunity.community_event
    RENAME TO event_community;

-- ── community_event_expense → event_expense ──────────────────────────────────
ALTER TABLE IF EXISTS manacommunity.community_event_expense
    RENAME TO event_expense;

-- ── community_event_task → event_task ────────────────────────────────────────
ALTER TABLE IF EXISTS manacommunity.community_event_task
    RENAME TO event_task;

-- ── community_event_sponsor → event_sponsor ──────────────────────────────────
ALTER TABLE IF EXISTS manacommunity.community_event_sponsor
    RENAME TO event_sponsor;

-- ── community_event_notification → event_scheduled_notification ──────────────
ALTER TABLE IF EXISTS manacommunity.community_event_notification
    RENAME TO event_scheduled_notification;

-- ── community_event_volunteer → event_volunteer ──────────────────────────────
ALTER TABLE IF EXISTS manacommunity.community_event_volunteer
    RENAME TO event_volunteer;

-- ── community_event_meal_registration → event_meal_registrations ─────────────
ALTER TABLE IF EXISTS manacommunity.community_event_meal_registration
    RENAME TO event_meal_registrations;

-- ── community_event_activity_registration → event_activity_registrations ─────
ALTER TABLE IF EXISTS manacommunity.community_event_activity_registration
    RENAME TO event_activity_registrations;

-- ── pooja_schedule → event_pooja_schedule ────────────────────────────────────
ALTER TABLE IF EXISTS manacommunity.pooja_schedule
    RENAME TO event_pooja_schedule;

-- ── pooja_slot_reservation → event_pooja_slot_reservation ────────────────────
ALTER TABLE IF EXISTS manacommunity.pooja_slot_reservation
    RENAME TO event_pooja_slot_reservation;
