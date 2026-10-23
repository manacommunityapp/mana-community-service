-- V8__ai_analytics_platform_schema.sql
-- Migration for AI Analytics and Platform Schema

CREATE TABLE ai_property_valuations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    predicted_value DECIMAL(15,2),
    confidence_score DECIMAL(5,2),
    value_range_low DECIMAL(15,2),
    value_range_high DECIMAL(15,2),
    valuation_date DATE,
    model_version VARCHAR(50),
    factors_json JSONB DEFAULT '{}',
    appreciation_1yr_pct DECIMAL(5,2),
    appreciation_3yr_pct DECIMAL(5,2),
    appreciation_5yr_pct DECIMAL(5,2),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE ai_rental_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    predicted_monthly_rent DECIMAL(10,2),
    confidence_score DECIMAL(5,2),
    rent_range_low DECIMAL(10,2),
    rent_range_high DECIMAL(10,2),
    gross_yield_pct DECIMAL(5,2),
    net_yield_pct DECIMAL(5,2),
    occupancy_rate_pct DECIMAL(5,2),
    prediction_date DATE,
    model_version VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE ai_investment_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    overall_score DECIMAL(5,2),
    score_date DATE,
    capital_appreciation_score DECIMAL(5,2),
    rental_yield_score DECIMAL(5,2),
    location_score DECIMAL(5,2),
    liquidity_score DECIMAL(5,2),
    risk_score DECIMAL(5,2),
    recommendation VARCHAR(50),
    insights_json JSONB DEFAULT '[]',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE ai_search_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    query_text TEXT,
    parsed_filters_json JSONB DEFAULT '{}',
    result_count INT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE market_comparisons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    comparison_date DATE,
    locality VARCHAR(255),
    avg_price_per_sqft DECIMAL(10,2),
    subject_price_per_sqft DECIMAL(10,2),
    premium_discount_pct DECIMAL(5,2),
    comparable_count INT,
    data_source VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE property_analytics_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID,
    snapshot_date DATE,
    total_properties INT,
    occupied_count INT,
    vacant_count INT,
    for_sale_count INT,
    for_rent_count INT,
    avg_property_value DECIMAL(15,2),
    avg_rental_yield DECIMAL(5,2),
    avg_maintenance_cost DECIMAL(10,2),
    occupancy_rate_pct DECIMAL(5,2),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_code VARCHAR(50) UNIQUE NOT NULL,
    plan_name VARCHAR(100),
    plan_type VARCHAR(50), -- COMMUNITY/BUILDER/BROKER/INDIVIDUAL
    price_monthly DECIMAL(10,2),
    price_annual DECIMAL(10,2),
    max_properties INT,
    max_users INT,
    features_json JSONB DEFAULT '[]',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Insert reference data
INSERT INTO subscription_plans (plan_code, plan_name, plan_type, price_monthly, price_annual, max_properties, max_users) VALUES 
    ('FREE', 'Free', 'INDIVIDUAL', 0, 0, 5, 10),
    ('PRO', 'Pro', 'BROKER', 2999, 29990, 100, 50),
    ('ENTERPRISE', 'Enterprise', 'COMMUNITY', 9999, 99990, -1, -1); -- Using -1 for unlimited

CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plan_code VARCHAR(50) REFERENCES subscription_plans(plan_code),
    start_date DATE,
    end_date DATE,
    status VARCHAR(50), -- ACTIVE/EXPIRED/CANCELLED
    auto_renew BOOLEAN DEFAULT true,
    payment_reference VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    title VARCHAR(255),
    body TEXT,
    notification_type VARCHAR(50), -- INFO/WARNING/ALERT/SUCCESS
    channel VARCHAR(50), -- EMAIL/SMS/PUSH/WHATSAPP
    entity_type VARCHAR(100),
    entity_id UUID,
    is_read BOOLEAN DEFAULT false,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_ai_property_valuations_tenant_id ON ai_property_valuations(tenant_id);
CREATE INDEX idx_ai_property_valuations_property_id ON ai_property_valuations(property_id);

CREATE INDEX idx_ai_rental_predictions_tenant_id ON ai_rental_predictions(tenant_id);
CREATE INDEX idx_ai_rental_predictions_property_id ON ai_rental_predictions(property_id);

CREATE INDEX idx_ai_investment_scores_tenant_id ON ai_investment_scores(tenant_id);
CREATE INDEX idx_ai_investment_scores_property_id ON ai_investment_scores(property_id);

CREATE INDEX idx_ai_search_queries_tenant_id ON ai_search_queries(tenant_id);
CREATE INDEX idx_ai_search_queries_user_id ON ai_search_queries(user_id);

CREATE INDEX idx_market_comparisons_tenant_id ON market_comparisons(tenant_id);
CREATE INDEX idx_market_comparisons_property_id ON market_comparisons(property_id);

CREATE INDEX idx_property_analytics_snapshots_tenant_id ON property_analytics_snapshots(tenant_id);
CREATE INDEX idx_property_analytics_snapshots_snapshot_date ON property_analytics_snapshots(snapshot_date);

CREATE INDEX idx_tenant_subscriptions_tenant_id ON tenant_subscriptions(tenant_id);

CREATE INDEX idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
