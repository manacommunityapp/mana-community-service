-- ============================================================================
-- V522: Ensure hr_offers.offered_salary remains NUMERIC(14,2)
-- Fixes databases where SchemaConstraintPatcher's legacy V335 block reverted
-- offered_salary back to DOUBLE PRECISION after V520.
-- ============================================================================

DO $$
BEGIN
    IF to_regclass('manacommunity.hr_offers') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'manacommunity'
             AND table_name = 'hr_offers'
             AND column_name = 'offered_salary'
             AND data_type IN ('double precision', 'real')
       ) THEN
        ALTER TABLE manacommunity.hr_offers
            ALTER COLUMN offered_salary TYPE NUMERIC(14,2)
            USING ROUND(offered_salary::numeric, 2);
    END IF;
END $$;
