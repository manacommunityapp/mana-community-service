-- Migration V549: Composite tenant indexes (community_id, <primary_lookup>) for 1,000+ community scale.
-- Prevents full table scans when shared multi-tenant tables grow across hundreds of communities.
-- Each index creation validates table + column existence in the active schema before running DDL.

DO $$
DECLARE
    idx RECORD;
    target_schema TEXT := current_schema();
    required_cols TEXT[];
    col TEXT;
    all_cols_exist BOOLEAN;
BEGIN
    FOR idx IN
        SELECT * FROM (VALUES
            -- Visitor & Gate operations
            ('idx_v549_visitor_pass_comm_flat_created', 'visitor_pass', ARRAY['community_id', 'flat_no', 'created_at'], 'community_id, flat_no, created_at DESC'),
            ('idx_v549_visitor_pass_comm_arrival',      'visitor_pass', ARRAY['community_id', 'expected_arrival_time'], 'community_id, expected_arrival_time DESC'),

            -- User directory & block/flat lookup
            ('idx_v549_app_user_comm_active_role',      'app_user',     ARRAY['community_id', 'is_active', 'role'], 'community_id, is_active, role'),
            ('idx_v549_app_user_comm_block_flat',       'app_user',     ARRAY['community_id', 'block', 'flat_no'], 'community_id, block, flat_no'),
            ('idx_v549_flat_memberships_comm_user',     'flat_memberships', ARRAY['community_id', 'user_id'], 'community_id, user_id'),

            -- Billing, Invoices & Payments
            ('idx_v549_invoice_comm_status',            'invoice',      ARRAY['community_id', 'status'], 'community_id, status'),
            ('idx_v549_invoice_comm_resident_created',  'invoice',      ARRAY['community_id', 'resident_id', 'created_at'], 'community_id, resident_id, created_at DESC'),
            ('idx_v549_invoices_comm_status_due',       'invoices',     ARRAY['community_id', 'status', 'due_date'], 'community_id, status, due_date'),
            ('idx_v549_billing_run_comm_status',        'billing_run',  ARRAY['community_id', 'status', 'created_at'], 'community_id, status, created_at DESC'),

            -- Helpdesk, Notices, Polls & Bookings
            ('idx_v549_tickets_comm_status_created',    'tickets',      ARRAY['community_id', 'status', 'created_at'], 'community_id, status, created_at DESC'),
            ('idx_v549_notices_comm_created',           'notices',      ARRAY['community_id', 'created_at'], 'community_id, created_at DESC'),
            ('idx_v549_poll_comm_status_created',       'poll',         ARRAY['community_id', 'status', 'created_at'], 'community_id, status, created_at DESC'),
            ('idx_v549_community_ballot_comm_status',   'community_ballot', ARRAY['community_id', 'status', 'created_at'], 'community_id, status, created_at DESC'),
            ('idx_v549_service_req_comm_status',        'service_requests', ARRAY['community_id', 'status', 'created_at'], 'community_id, status, created_at DESC'),
            ('idx_v549_resource_booking_comm_res',      'resource_booking', ARRAY['community_id', 'resource_id'], 'community_id, resource_id'),

            -- RBAC, Modules, Block Config & Tenant Quotas (Onboarding & Fast Lookup)
            ('idx_v549_roles_comm_name',                'roles',        ARRAY['community_id', 'name'], 'community_id, name'),
            ('idx_v549_community_modules_comm_enabled', 'community_modules', ARRAY['community_id', 'is_enabled', 'sort_order'], 'community_id, is_enabled, sort_order'),
            ('idx_v549_block_config_comm_block',        'community_block_config', ARRAY['community_id', 'block_name'], 'community_id, block_name'),
            ('idx_v549_tenant_quota_comm_active_type',  'tenant_resource_quota', ARRAY['community_id', 'active', 'resource_type'], 'community_id, active, resource_type')
        ) AS t(index_name, tbl_name, col_names, index_expr)
    LOOP
        all_cols_exist := TRUE;
        FOREACH col IN ARRAY idx.col_names LOOP
            IF NOT EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = target_schema
                  AND table_name = idx.tbl_name
                  AND column_name = col
            ) THEN
                all_cols_exist := FALSE;
                EXIT;
            END IF;
        END LOOP;

        IF all_cols_exist THEN
            EXECUTE format(
                'CREATE INDEX IF NOT EXISTS %I ON %I.%I (%s)',
                idx.index_name, target_schema, idx.tbl_name, idx.index_expr
            );
        END IF;
    END LOOP;
END $$;
