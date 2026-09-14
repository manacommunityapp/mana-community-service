-- Tier 2 BaseAuditEntity: batch 2 — add created_by / updated_by columns

ALTER TABLE manacommunity.vms_goods_receipts
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.vms_settlements
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- vms_purchase_requests already has created_by as FK column
ALTER TABLE manacommunity.vms_purchase_requests
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.service_request
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.work_order
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.family_members
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.marketplace_listings
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.marketplace_orders
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;
