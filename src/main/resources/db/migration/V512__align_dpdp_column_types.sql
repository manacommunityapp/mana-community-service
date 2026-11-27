-- =============================================================================
-- V512__align_dpdp_column_types.sql
-- Align PostgreSQL column data types with JPA entity mappings:
-- 1. health_data_access_log: change ip_address from INET to VARCHAR(50)
-- 2. user_consent_record: change ip_address from INET to VARCHAR(50)
-- 3. user_consent_record: change purpose from ENUM consent_purpose_code to VARCHAR(50)
-- =============================================================================

ALTER TABLE IF EXISTS health_data_access_log
    ALTER COLUMN ip_address TYPE VARCHAR(50);

ALTER TABLE IF EXISTS user_consent_record
    ALTER COLUMN ip_address TYPE VARCHAR(50),
    ALTER COLUMN purpose TYPE VARCHAR(50);
