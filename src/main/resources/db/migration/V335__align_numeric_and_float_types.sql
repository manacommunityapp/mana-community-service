-- V335: Align column types between JPA entities and database for Float/Double types
-- Converts columns mapped to Double in JPA entities to DOUBLE PRECISION in PostgreSQL.
-- All statements are safe, idempotent, and non-destructive.

DO $$
BEGIN
  -- 1. hr_attendance_logs: latitude & longitude
  IF to_regclass('manacommunity.hr_attendance_logs') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_attendance_logs' AND column_name='latitude') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_attendance_logs ALTER COLUMN latitude TYPE DOUBLE PRECISION USING latitude::DOUBLE PRECISION';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_attendance_logs' AND column_name='longitude') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_attendance_logs ALTER COLUMN longitude TYPE DOUBLE PRECISION USING longitude::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 2. hr_job_postings: min_salary & max_salary
  IF to_regclass('manacommunity.hr_job_postings') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_job_postings' AND column_name='min_salary') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_job_postings ALTER COLUMN min_salary TYPE DOUBLE PRECISION USING min_salary::DOUBLE PRECISION';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_job_postings' AND column_name='max_salary') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_job_postings ALTER COLUMN max_salary TYPE DOUBLE PRECISION USING max_salary::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 3. hr_leave_balances: total_balance, used_balance, remaining_balance
  IF to_regclass('manacommunity.hr_leave_balances') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_leave_balances' AND column_name='total_balance') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_leave_balances ALTER COLUMN total_balance TYPE DOUBLE PRECISION USING total_balance::DOUBLE PRECISION';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_leave_balances' AND column_name='used_balance') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_leave_balances ALTER COLUMN used_balance TYPE DOUBLE PRECISION USING used_balance::DOUBLE PRECISION';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_leave_balances' AND column_name='remaining_balance') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_leave_balances ALTER COLUMN remaining_balance TYPE DOUBLE PRECISION USING remaining_balance::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 4. hr_leave_requests: total_days
  IF to_regclass('manacommunity.hr_leave_requests') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_leave_requests' AND column_name='total_days') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_leave_requests ALTER COLUMN total_days TYPE DOUBLE PRECISION USING total_days::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 5. hr_offers: offered_salary
  IF to_regclass('manacommunity.hr_offers') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_offers' AND column_name='offered_salary') THEN
      EXECUTE 'ALTER TABLE manacommunity.hr_offers ALTER COLUMN offered_salary TYPE DOUBLE PRECISION USING offered_salary::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 6. post: price
  IF to_regclass('manacommunity.post') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post' AND column_name='price') THEN
      EXECUTE 'ALTER TABLE manacommunity.post ALTER COLUMN price TYPE DOUBLE PRECISION USING price::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 7. group_buy_deal: vendor_rating
  IF to_regclass('manacommunity.group_buy_deal') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='group_buy_deal' AND column_name='vendor_rating') THEN
      EXECUTE 'ALTER TABLE manacommunity.group_buy_deal ALTER COLUMN vendor_rating TYPE DOUBLE PRECISION USING vendor_rating::DOUBLE PRECISION';
    END IF;
  END IF;

  -- 8. group_buy_demand_offer: vendor_rating
  IF to_regclass('manacommunity.group_buy_demand_offer') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='group_buy_demand_offer' AND column_name='vendor_rating') THEN
      EXECUTE 'ALTER TABLE manacommunity.group_buy_demand_offer ALTER COLUMN vendor_rating TYPE DOUBLE PRECISION USING vendor_rating::DOUBLE PRECISION';
    END IF;
  END IF;
END $$;
