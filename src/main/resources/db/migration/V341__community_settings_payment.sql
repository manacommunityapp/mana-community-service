ALTER TABLE community_settings
    ADD COLUMN IF NOT EXISTS allowed_payment_gateways  VARCHAR(20)[]  NOT NULL DEFAULT '{CASHFREE}',
    ADD COLUMN IF NOT EXISTS manual_payment_enabled     BOOLEAN        NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS manual_payment_modes       VARCHAR(20)[]  NOT NULL DEFAULT '{CASH,UPI}',
    ADD COLUMN IF NOT EXISTS payment_instructions       VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS default_gateway            VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cashier_can_record_payment BOOLEAN        NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS fallback_gateway           VARCHAR(20);

COMMENT ON COLUMN community_settings.allowed_payment_gateways IS
    'Subset of platform_payment_config enabled gateways this community accepts';
COMMENT ON COLUMN community_settings.manual_payment_enabled IS
    'Whether community admin/cashier can record offline cash/cheque payments';
