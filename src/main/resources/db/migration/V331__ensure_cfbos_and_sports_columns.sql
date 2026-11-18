-- V331: Ensure CFBOS and sports columns/tables exist
-- Fixes missing columns on environments where earlier migrations (V100, V101, V244, V245, V246)
-- were skipped due to historical Flyway duplicate version collisions.

-- 1. CFBOS Expense multi-tenancy and vendor link
ALTER TABLE manacommunity.cfbos_expense
    ADD COLUMN IF NOT EXISTS community_id BIGINT,
    ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT;
UPDATE manacommunity.cfbos_expense SET community_id = 1 WHERE community_id IS NULL;
ALTER TABLE manacommunity.cfbos_expense ALTER COLUMN community_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_cfbos_expense_community_id ON manacommunity.cfbos_expense (community_id);

-- 2. CFBOS Invoice generalized schema
DO $$
BEGIN
  IF to_regclass('manacommunity.cfbos_invoice') IS NOT NULL THEN
    ALTER TABLE manacommunity.cfbos_invoice ALTER COLUMN property_id DROP NOT NULL;
    ALTER TABLE manacommunity.cfbos_invoice ALTER COLUMN resident_id DROP NOT NULL;
    ALTER TABLE manacommunity.cfbos_invoice
        ADD COLUMN IF NOT EXISTS community_id BIGINT,
        ADD COLUMN IF NOT EXISTS source_module VARCHAR(30),
        ADD COLUMN IF NOT EXISTS source_reference_type VARCHAR(50),
        ADD COLUMN IF NOT EXISTS source_reference_id BIGINT;
    UPDATE manacommunity.cfbos_invoice SET community_id = 1 WHERE community_id IS NULL;
    CREATE INDEX IF NOT EXISTS idx_cfbos_invoice_community_id ON manacommunity.cfbos_invoice (community_id);
    CREATE INDEX IF NOT EXISTS idx_cfbos_invoice_source ON manacommunity.cfbos_invoice (source_module, source_reference_type, source_reference_id);
  END IF;
END $$;

-- 3. CFBOS Account Group audit columns
DO $$
BEGIN
  IF to_regclass('manacommunity.cfbos_account_group') IS NOT NULL THEN
    ALTER TABLE manacommunity.cfbos_account_group
        ADD COLUMN IF NOT EXISTS created_by BIGINT,
        ADD COLUMN IF NOT EXISTS updated_by BIGINT,
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
  END IF;
END $$;

-- 4. Community Settings accounting freeze
DO $$
BEGIN
  IF to_regclass('manacommunity.community_settings') IS NOT NULL THEN
    ALTER TABLE manacommunity.community_settings
        ADD COLUMN IF NOT EXISTS accounting_freeze_date DATE,
        ADD COLUMN IF NOT EXISTS accounting_freeze_note TEXT;
  END IF;
END $$;

-- 5. Sports Tournament Config advanced scheduling and audit fields
DO $$
BEGIN
  IF to_regclass('manacommunity.sports_tournament_config') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_tournament_config
        ADD COLUMN IF NOT EXISTS seeding_order VARCHAR(30) DEFAULT 'RANDOM',
        ADD COLUMN IF NOT EXISTS byes_first_round_only BOOLEAN DEFAULT TRUE,
        ADD COLUMN IF NOT EXISTS show_byes_in_bracket BOOLEAN DEFAULT TRUE,
        ADD COLUMN IF NOT EXISTS limit_matches_per_day BOOLEAN DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS max_matches_per_day INT DEFAULT 2,
        ADD COLUMN IF NOT EXISTS use_playoff_days BOOLEAN DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS playoff_days VARCHAR(50),
        ADD COLUMN IF NOT EXISTS venue_assign_type VARCHAR(30) DEFAULT 'Random',
        ADD COLUMN IF NOT EXISTS playoff_round_timings_json TEXT,
        ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT;
  END IF;
END $$;

-- 6. Sports Tournament Match match-config and feed fields
DO $$
BEGIN
  IF to_regclass('manacommunity.sports_tournament_match') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_tournament_match
        ADD COLUMN IF NOT EXISTS match_config_override_json TEXT,
        ADD COLUMN IF NOT EXISTS winner_name VARCHAR(150),
        ADD COLUMN IF NOT EXISTS winner_side VARCHAR(20),
        ADD COLUMN IF NOT EXISTS show_in_feed BOOLEAN DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS feed_result_post_id BIGINT,
        ADD COLUMN IF NOT EXISTS post_to_feed BOOLEAN,
        ADD COLUMN IF NOT EXISTS stage_id BIGINT,
        ADD COLUMN IF NOT EXISTS schedule_version_id BIGINT,
        ADD COLUMN IF NOT EXISTS is_reserve_day_match BOOLEAN DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS postponed_from_match_id BIGINT,
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
  END IF;
END $$;

-- 7. Sports Event / Tournament feed and nomination fields
DO $$
BEGIN
  IF to_regclass('manacommunity.sports_event') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_event
        ADD COLUMN IF NOT EXISTS captain_nomination_open BOOLEAN DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS show_in_feed BOOLEAN DEFAULT TRUE,
        ADD COLUMN IF NOT EXISTS feed_post_policy VARCHAR(50),
        ADD COLUMN IF NOT EXISTS feed_post_stages TEXT,
        ADD COLUMN IF NOT EXISTS auction BOOLEAN DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS allow_higher_age_category BOOLEAN DEFAULT TRUE;
  END IF;
  IF to_regclass('manacommunity.sports_tournament') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_tournament
        ADD COLUMN IF NOT EXISTS show_in_feed BOOLEAN DEFAULT TRUE,
        ADD COLUMN IF NOT EXISTS feed_post_policy VARCHAR(50),
        ADD COLUMN IF NOT EXISTS feed_post_stages TEXT,
        ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT;
  END IF;
END $$;

-- 8. Sports Scoring Config and Meta fields
DO $$
BEGIN
  IF to_regclass('manacommunity.sports_scoring_config') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_scoring_config
        ADD COLUMN IF NOT EXISTS overs_per_innings INT;
  END IF;
  IF to_regclass('manacommunity.sports_meta') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_meta
        ADD COLUMN IF NOT EXISTS icon_url VARCHAR(500);
  END IF;
END $$;

-- 9. Sports Tournament Player Payment collector columns
DO $$
BEGIN
  IF to_regclass('manacommunity.sports_tournament_player_payment') IS NOT NULL THEN
    ALTER TABLE manacommunity.sports_tournament_player_payment
        ADD COLUMN IF NOT EXISTS collected_by_name VARCHAR(150),
        ADD COLUMN IF NOT EXISTS collected_by_contact VARCHAR(150);
  END IF;
END $$;

-- 10. Community Market Bazaar and Budget tables (guarded creation)
DO $$
BEGIN
  IF to_regclass('manacommunity.market_event_bazaar') IS NULL AND to_regclass('manacommunity.event_community') IS NOT NULL THEN
    CREATE TABLE manacommunity.market_event_bazaar (
        id BIGINT PRIMARY KEY REFERENCES manacommunity.event_community(id) ON DELETE CASCADE,
        status VARCHAR(24) NOT NULL DEFAULT 'PUBLISHED',
        banner_url VARCHAR(1000),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    CREATE INDEX IF NOT EXISTS idx_market_event_bazaar_status ON manacommunity.market_event_bazaar(status);
  END IF;

  IF to_regclass('manacommunity.market_stall') IS NULL AND to_regclass('manacommunity.market_event_bazaar') IS NOT NULL THEN
    CREATE TABLE manacommunity.market_stall (
        id BIGSERIAL PRIMARY KEY,
        event_id BIGINT NOT NULL REFERENCES manacommunity.market_event_bazaar(id) ON DELETE CASCADE,
        stall_number VARCHAR(40) NOT NULL,
        zone VARCHAR(100),
        package_name VARCHAR(100) NOT NULL,
        description TEXT,
        size VARCHAR(100),
        price NUMERIC(12,2) NOT NULL CHECK (price >= 1.00),
        currency VARCHAR(5) NOT NULL DEFAULT 'INR',
        facilities TEXT,
        allowed_categories TEXT,
        floor_row INTEGER CHECK (floor_row IS NULL OR floor_row > 0),
        floor_column INTEGER CHECK (floor_column IS NULL OR floor_column > 0),
        status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT uq_market_stall_number UNIQUE(event_id, stall_number)
    );
    CREATE INDEX IF NOT EXISTS idx_market_stall_event_status ON manacommunity.market_stall(event_id, status);
  END IF;

  IF to_regclass('manacommunity.market_application') IS NULL AND to_regclass('manacommunity.market_stall') IS NOT NULL THEN
    CREATE TABLE manacommunity.market_application (
        id BIGSERIAL PRIMARY KEY,
        event_id BIGINT NOT NULL REFERENCES manacommunity.market_event_bazaar(id) ON DELETE CASCADE,
        stall_id BIGINT NOT NULL REFERENCES manacommunity.market_stall(id) ON DELETE CASCADE,
        applicant_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
        vendor_name VARCHAR(150) NOT NULL,
        business_name VARCHAR(150) NOT NULL,
        category VARCHAR(100) NOT NULL,
        phone VARCHAR(30) NOT NULL,
        products TEXT NOT NULL,
        compliance_documents TEXT,
        status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED',
        rejection_reason VARCHAR(1000),
        requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    CREATE INDEX IF NOT EXISTS idx_market_application_vendor ON manacommunity.market_application(applicant_id, requested_at DESC);
    CREATE INDEX IF NOT EXISTS idx_market_application_event_status ON manacommunity.market_application(event_id, status);
  END IF;

  IF to_regclass('manacommunity.market_booking') IS NULL AND to_regclass('manacommunity.market_application') IS NOT NULL THEN
    CREATE TABLE manacommunity.market_booking (
        id BIGSERIAL PRIMARY KEY,
        application_id BIGINT NOT NULL UNIQUE REFERENCES manacommunity.market_application(id) ON DELETE CASCADE,
        stall_id BIGINT NOT NULL REFERENCES manacommunity.market_stall(id) ON DELETE CASCADE,
        applicant_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
        amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
        currency VARCHAR(5) NOT NULL DEFAULT 'INR',
        status VARCHAR(24) NOT NULL DEFAULT 'PAYMENT_PENDING',
        hold_expires_at TIMESTAMP NOT NULL,
        payment_order_id VARCHAR(50),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    CREATE INDEX IF NOT EXISTS idx_market_booking_status_expiry ON manacommunity.market_booking(status, hold_expires_at);
    CREATE INDEX IF NOT EXISTS idx_market_booking_stall_status ON manacommunity.market_booking(stall_id, status);
  END IF;

  IF to_regclass('manacommunity.event_budget') IS NULL AND to_regclass('manacommunity.event_community') IS NOT NULL THEN
    CREATE TABLE manacommunity.event_budget (
        id          BIGSERIAL PRIMARY KEY,
        event_id    BIGINT NOT NULL REFERENCES manacommunity.event_community(id) ON DELETE CASCADE,
        community_id BIGINT NOT NULL,
        total_planned_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
        currency    VARCHAR(3) NOT NULL DEFAULT 'INR',
        notes       VARCHAR(1000),
        created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        CONSTRAINT uq_event_budget_event UNIQUE (event_id)
    );
    CREATE INDEX IF NOT EXISTS idx_event_budget_community ON manacommunity.event_budget(community_id);
  END IF;

  IF to_regclass('manacommunity.event_budget_line_item') IS NULL AND to_regclass('manacommunity.event_budget') IS NOT NULL THEN
    CREATE TABLE manacommunity.event_budget_line_item (
        id          BIGSERIAL PRIMARY KEY,
        budget_id   BIGINT NOT NULL REFERENCES manacommunity.event_budget(id) ON DELETE CASCADE,
        category    VARCHAR(100) NOT NULL,
        description VARCHAR(500),
        planned_amount NUMERIC(14,2) NOT NULL,
        created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS idx_event_budget_line_budget ON manacommunity.event_budget_line_item(budget_id);
  END IF;

  IF to_regclass('manacommunity.tournament_budget') IS NULL AND to_regclass('manacommunity.sports_tournament') IS NOT NULL THEN
    CREATE TABLE manacommunity.tournament_budget (
        id              BIGSERIAL PRIMARY KEY,
        tournament_id   BIGINT NOT NULL REFERENCES manacommunity.sports_tournament(id) ON DELETE CASCADE,
        community_id    BIGINT NOT NULL,
        total_planned_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
        currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
        notes           VARCHAR(1000),
        created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        CONSTRAINT uq_tournament_budget_tournament UNIQUE (tournament_id)
    );
    CREATE INDEX IF NOT EXISTS idx_tournament_budget_community ON manacommunity.tournament_budget(community_id);
  END IF;

  IF to_regclass('manacommunity.tournament_budget_line_item') IS NULL AND to_regclass('manacommunity.tournament_budget') IS NOT NULL THEN
    CREATE TABLE manacommunity.tournament_budget_line_item (
        id          BIGSERIAL PRIMARY KEY,
        budget_id   BIGINT NOT NULL REFERENCES manacommunity.tournament_budget(id) ON DELETE CASCADE,
        category    VARCHAR(100) NOT NULL,
        description VARCHAR(500),
        planned_amount NUMERIC(14,2) NOT NULL,
        created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS idx_tournament_budget_line_budget ON manacommunity.tournament_budget_line_item(budget_id);
  END IF;
END $$;
