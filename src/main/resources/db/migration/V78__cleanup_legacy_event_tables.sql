-- V78: Drop old legacy table names if they still exist anywhere to avoid ambiguity.

DROP TABLE IF EXISTS manacommunity.community_event_activity_registration CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event_meal_registration CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event_notification CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event_volunteer CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event_sponsor CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event_expense CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event_task CASCADE;
DROP TABLE IF EXISTS manacommunity.pooja_slot_reservation CASCADE;
DROP TABLE IF EXISTS manacommunity.pooja_schedule CASCADE;
DROP TABLE IF EXISTS manacommunity.community_event CASCADE;

DROP TABLE IF EXISTS public.community_event_activity_registration CASCADE;
DROP TABLE IF EXISTS public.community_event_meal_registration CASCADE;
DROP TABLE IF EXISTS public.community_event_notification CASCADE;
DROP TABLE IF EXISTS public.community_event_volunteer CASCADE;
DROP TABLE IF EXISTS public.community_event_sponsor CASCADE;
DROP TABLE IF EXISTS public.community_event_expense CASCADE;
DROP TABLE IF EXISTS public.community_event_task CASCADE;
DROP TABLE IF EXISTS public.pooja_slot_reservation CASCADE;
DROP TABLE IF EXISTS public.pooja_schedule CASCADE;
DROP TABLE IF EXISTS public.community_event CASCADE;
