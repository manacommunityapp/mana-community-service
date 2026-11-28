-- Tenant Row-Level Security (RLS) foundation & isolation indexes across core multi-tenant tables.
-- Uses session parameter app.current_community_id set per transaction via HibernateTenantFilterAspect.
-- When app.current_community_id is unset (NULL / empty for background system jobs), rows remain
-- accessible to the service role; when set, PostgreSQL strictly limits rows to matching community_id.

CREATE OR REPLACE FUNCTION manacommunity.current_tenant_community_id()
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    raw_val TEXT;
BEGIN
    raw_val := NULLIF(current_setting('app.current_community_id', true), '');
    IF raw_val IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN raw_val::BIGINT;
EXCEPTION
    WHEN invalid_text_representation THEN
        RETURN NULL;
END;
$$;

DO $$
DECLARE
    tbl TEXT;
    target_tables TEXT[] := ARRAY[
        'visitor_pass',
        'tickets',
        'notices',
        'invoices',
        'poll',
        'market_listings',
        'market_seller_profiles',
        'service_requests',
        'prepaid_meter_accounts',
        'move_out_sagas',
        'move_slots'
    ];
BEGIN
    FOREACH tbl IN ARRAY target_tables LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'manacommunity'
              AND table_name = tbl
              AND column_name = 'community_id'
        ) THEN
            EXECUTE format('ALTER TABLE manacommunity.%I ENABLE ROW LEVEL SECURITY', tbl);
            EXECUTE format('DROP POLICY IF EXISTS tenant_community_isolation_policy ON manacommunity.%I', tbl);
            EXECUTE format(
                'CREATE POLICY tenant_community_isolation_policy ON manacommunity.%I ' ||
                'USING (manacommunity.current_tenant_community_id() IS NULL OR community_id = manacommunity.current_tenant_community_id()) ' ||
                'WITH CHECK (manacommunity.current_tenant_community_id() IS NULL OR community_id = manacommunity.current_tenant_community_id())',
                tbl
            );
        END IF;
    END LOOP;
END $$;
