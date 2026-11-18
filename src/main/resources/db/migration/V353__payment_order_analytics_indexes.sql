-- Composite indexes to support analytics queries that filter by community + status/date.
-- These avoid full scans on payment_order when reporting per-community KPIs.

CREATE INDEX IF NOT EXISTS idx_payment_order_community_status
    ON payment_order (community_id, status);

CREATE INDEX IF NOT EXISTS idx_payment_order_community_created
    ON payment_order (community_id, created_at);
