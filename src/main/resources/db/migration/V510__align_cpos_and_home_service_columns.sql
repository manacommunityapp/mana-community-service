-- =============================================================================
-- V510__align_cpos_and_home_service_columns.sql
-- Add missing columns to align JPA entity definitions with database schema:
-- 1. cpos_broker_commission: paid_at, payment_reference, payment_method, gl_voucher_number
-- 2. home_service_bookings: completion_otp
-- 3. home_service_workers: vms_vendor_id
-- 4. hs_worker: vms_vendor_id, user_id
-- =============================================================================

-- 1. cpos_broker_commission (com.manacommunity.api.model.CposBrokerCommission)
ALTER TABLE IF EXISTS cpos_broker_commission
    ADD COLUMN IF NOT EXISTS paid_at           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_method    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS gl_voucher_number VARCHAR(60);

-- 2. home_service_bookings (com.manacommunity.api.homeservice.model.entity.HomeServiceBookingEntity)
ALTER TABLE IF EXISTS home_service_bookings
    ADD COLUMN IF NOT EXISTS completion_otp VARCHAR(6);

-- 3. home_service_workers (com.manacommunity.api.homeservice.model.entity.HomeServiceWorkerEntity)
ALTER TABLE IF EXISTS home_service_workers
    ADD COLUMN IF NOT EXISTS vms_vendor_id BIGINT;

-- 4. hs_worker (com.manacommunity.api.homeservices.entity.HsWorker)
ALTER TABLE IF EXISTS hs_worker
    ADD COLUMN IF NOT EXISTS vms_vendor_id BIGINT,
    ADD COLUMN IF NOT EXISTS user_id       BIGINT;
