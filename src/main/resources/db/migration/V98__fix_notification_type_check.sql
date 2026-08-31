-- V98: Sync notification.type CHECK constraint with the NotificationType enum.
--
-- The constraint was created before SIGNUP_SUCCESS and PASSWORD_RESET were added
-- to the enum, causing a 23514 violation on /api/auth/register.
-- Fix: drop the old constraint and recreate it with the full enum value set.

ALTER TABLE manacommunity.notification
    DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE manacommunity.notification
    ADD CONSTRAINT notification_type_check CHECK (type IN (
        -- Sports — schedule & match lifecycle
        'SCHEDULE_PUBLISHED',
        'SCHEDULE_UPDATED',
        'MATCH_REMINDER',
        'MATCH_RESULT_POSTED',

        -- Registration lifecycle
        'REGISTRATION_RECEIVED',
        'REGISTRATION_OPEN',
        'REGISTRATION_CONFIRMED',
        'REGISTRATION_REJECTED',
        'REGISTRATION_WITHDRAWN',

        -- Event lifecycle
        'EVENT_UPDATED',
        'EVENT_CANCELLED',
        'EVENT_STATUS_CHANGED',

        -- Auction lifecycle
        'AUCTION_STARTED',
        'AUCTION_COMPLETED',
        'PLAYER_SOLD',
        'BID_OUTBID',

        -- Team & captain
        'TEAM_ASSIGNED',
        'TEAM_CREATED',
        'CAPTAIN_NOMINATED',
        'CAPTAIN_CONFIRMED',

        -- Tournament results
        'WINNER_NOTIFICATION',
        'TOURNAMENT_COMPLETED',
        'PRIZE_DISTRIBUTION',

        -- Tournament announcements
        'TOURNAMENT_OPEN',
        'TOURNAMENT_ANNOUNCEMENT',

        -- General
        'GENERAL',

        -- Visitor
        'VISITOR_PENDING',
        'VISITOR_CHECK_IN',

        -- Account
        'SIGNUP_SUCCESS',
        'PASSWORD_RESET'
    ));
