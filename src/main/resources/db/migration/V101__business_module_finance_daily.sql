CREATE TABLE IF NOT EXISTS business_module_finance_daily (
    id BIGSERIAL PRIMARY KEY,
    module_code VARCHAR(60) NOT NULL,
    report_date DATE NOT NULL,
    commission_revenue NUMERIC(14,2) NOT NULL,
    direct_cost NUMERIC(14,2) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_business_module_finance_daily UNIQUE (module_code, report_date)
);

CREATE INDEX IF NOT EXISTS idx_business_module_finance_daily_date
    ON business_module_finance_daily(report_date);
