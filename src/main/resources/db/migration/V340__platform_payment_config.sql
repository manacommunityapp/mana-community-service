CREATE TABLE platform_payment_config (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cashfree_enabled        BOOLEAN NOT NULL DEFAULT false,
    razorpay_enabled        BOOLEAN NOT NULL DEFAULT false,
    mana_pay_enabled        BOOLEAN NOT NULL DEFAULT false,
    bbps_enabled            BOOLEAN NOT NULL DEFAULT false,
    default_gateway         VARCHAR(20) NOT NULL DEFAULT 'CASHFREE',
    convenience_fee_percent NUMERIC(5,4) NOT NULL DEFAULT 0,
    max_transaction_amount  NUMERIC(14,2),
    updated_by              BIGINT,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_default_gateway CHECK (
        default_gateway IN ('CASHFREE','RAZORPAY','MANA_PAY','UPI_DIRECT','MANUAL')
    )
);

-- Seed one platform-wide config row
INSERT INTO platform_payment_config (cashfree_enabled, razorpay_enabled, mana_pay_enabled, bbps_enabled, default_gateway)
VALUES (true, false, false, false, 'CASHFREE');
