-- V336: Align Integer and Smallint column types between JPA entities and database
-- Resolves hr_goals.weightage decimal-to-integer mismatch and widens legacy smallint columns to INT.
-- All statements are safe, idempotent, and non-destructive.

DO $$
BEGIN
  -- 1. hr_goals: weightage
  IF to_regclass('manacommunity.hr_goals') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_goals' AND column_name='weightage') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_goals ALTER COLUMN weightage TYPE INT USING ROUND(COALESCE(weightage, 0))::INT';
    END IF;
  END IF;

  -- 2. community_block_config: total_floors, flats_per_floor
  IF to_regclass('manacommunity.community_block_config') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='community_block_config' AND column_name='total_floors') THEN
      EXECUTE 'ALTER TABLE manacommunity.community_block_config ALTER COLUMN total_floors TYPE INT';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='community_block_config' AND column_name='flats_per_floor') THEN
      EXECUTE 'ALTER TABLE manacommunity.community_block_config ALTER COLUMN flats_per_floor TYPE INT';
    END IF;
  END IF;

  -- 3. cpos_crm_sales_target: period_month, period_year
  IF to_regclass('manacommunity.cpos_crm_sales_target') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='cpos_crm_sales_target' AND column_name='period_month') THEN
      EXECUTE 'ALTER TABLE manacommunity.cpos_crm_sales_target ALTER COLUMN period_month TYPE INT';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='cpos_crm_sales_target' AND column_name='period_year') THEN
      EXECUTE 'ALTER TABLE manacommunity.cpos_crm_sales_target ALTER COLUMN period_year TYPE INT';
    END IF;
  END IF;

  -- 4. mobility_airport_ride_request: time_window_minutes
  IF to_regclass('manacommunity.mobility_airport_ride_request') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='mobility_airport_ride_request' AND column_name='time_window_minutes') THEN
      EXECUTE 'ALTER TABLE manacommunity.mobility_airport_ride_request ALTER COLUMN time_window_minutes TYPE INT';
    END IF;
  END IF;
END $$;
