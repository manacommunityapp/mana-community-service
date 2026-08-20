-- V62: Add community_id scoping to vendor invoices, budget allocations, and purchase requests
-- This prevents cross-tenant data leaks (IDOR/parameter tampering vulnerabilities).

-- 1. vendor invoices (table: invoices)
ALTER TABLE manacommunity.invoices
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id);

CREATE INDEX IF NOT EXISTS idx_invoices_community_id ON manacommunity.invoices(community_id);

-- 2. budget allocations
ALTER TABLE manacommunity.budget_allocations
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id);

-- Drop old unique constraint (financial_year, category) and replace with community-scoped one
ALTER TABLE manacommunity.budget_allocations
    DROP CONSTRAINT IF EXISTS uk_budget_financial_year_category;

ALTER TABLE manacommunity.budget_allocations
    ADD CONSTRAINT uk_budget_community_year_category
        UNIQUE (community_id, financial_year, category);

CREATE INDEX IF NOT EXISTS idx_budget_allocations_community_id ON manacommunity.budget_allocations(community_id);

-- 3. purchase requests
ALTER TABLE manacommunity.purchase_requests
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id);

CREATE INDEX IF NOT EXISTS idx_purchase_requests_community_id ON manacommunity.purchase_requests(community_id);
