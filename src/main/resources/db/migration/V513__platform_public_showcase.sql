-- =============================================================================
-- V513__platform_public_showcase.sql
-- Dynamic public landing / onboarding configuration and showcase cards
-- Read by GET /api/communities/public/overview and editable by SUPER_ADMIN.
-- =============================================================================

CREATE TABLE IF NOT EXISTS platform_public_config (
    id                 BIGSERIAL PRIMARY KEY,
    config_key         VARCHAR(40)  NOT NULL UNIQUE,
    top_badge_text     VARCHAR(80)  NOT NULL,
    hero_capsule_text  VARCHAR(80)  NOT NULL,
    hero_title         VARCHAR(80)  NOT NULL,
    hero_subtitle      VARCHAR(255) NOT NULL,
    security_label     VARCHAR(80)  NOT NULL,
    certification_note VARCHAR(160) NOT NULL,
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform_showcase_item (
    id           BIGSERIAL PRIMARY KEY,
    item_key     VARCHAR(50)  NOT NULL UNIQUE,
    emoji        VARCHAR(16)  NOT NULL,
    accent_color VARCHAR(20)  NOT NULL,
    title        VARCHAR(100) NOT NULL,
    tagline      VARCHAR(120) NOT NULL,
    description  VARCHAR(320) NOT NULL,
    tags_csv     VARCHAR(255) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO platform_public_config (
    config_key, top_badge_text, hero_capsule_text, hero_title,
    hero_subtitle, security_label, certification_note
) VALUES (
    'default',
    'Verified Gated Communities',
    'ELEVATING GATED LIVING',
    'Mana Community',
    'Your home, neighbors, and society operations — seamlessly connected in your pocket.',
    'Bank-Grade Secure',
    'Encrypted · RWA & Apartment Association Certified'
) ON CONFLICT (config_key) DO NOTHING;

INSERT INTO platform_showcase_item (
    item_key, emoji, accent_color, title, tagline, description, tags_csv, sort_order, active
) VALUES
    ('security', '🛡️', '#10B981', 'Smart Gate & 24/7 Security', 'Instant QR Entry Passes & SOS Alert',
     'Digital gate passes for visitors, delivery PINs, automated staff clock-in, and one-tap emergency guard hotline.',
     'Guest QR Pass,Delivery PIN,Maid Log,Emergency SOS', 1, TRUE),
    ('sports', '🏟️', '#F59E0B', 'Sports Arena & Tournaments', 'Live Scores · Court Booking · Leagues',
     'Book clubhouse courts in seconds, register for community tournaments, and track real-time scoreboards.',
     'Court Booking,Live Scoreboard,Society Cups,Player Ratings', 2, TRUE),
    ('marketplace', '🛒', '#A855F7', 'Local Market & Live Auctions', 'Buy, Sell & Save with Verified Neighbors',
     'Trade pre-loved goods, bid in resident auctions, and unlock bulk discounts with community group buying.',
     'Classifieds,Live Auctions,Group Deals,Free Giveaways', 3, TRUE),
    ('living', '⚡', '#06B6D4', 'Maintenance Dues & Amenities', 'Seamless Clubhouse Living',
     'Pay monthly maintenance in one tap, reserve party halls, and track helpdesk tickets with automated resolution SLAs.',
     'Maintenance Pay,Clubhouse Booking,Quick Helpdesk,Polls & Notices', 4, TRUE)
ON CONFLICT (item_key) DO NOTHING;
