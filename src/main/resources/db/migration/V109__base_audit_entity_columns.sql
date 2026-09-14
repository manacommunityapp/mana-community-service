-- V109: Tier 2 audit columns — add created_by / updated_by (and updated_at where missing)
-- for entities that now extend BaseAuditEntity.
--
-- All columns are nullable:
--   • existing rows receive NULL (no historical actor data available)
--   • background-thread writes (schedulers, system operations) also produce NULL
-- The FK to app_user is intentionally omitted here — BaseAuditEntity writes a plain
-- Long via @CreatedBy / @LastModifiedBy; enforcement is at application level.

-- ─── Finance ─────────────────────────────────────────────────────────────────

ALTER TABLE manacommunity.finance_credit_note
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.finance_debit_note
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ─── CFBOS Accounting ────────────────────────────────────────────────────────

-- cfbos_account_group had only created_at; BaseAuditEntity also provides updated_at.
ALTER TABLE manacommunity.cfbos_account_group
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP;

-- cfbos_cost_center already has created_by/updated_by as plain BIGINT columns
-- (from the former inline Long fields). No column changes needed; Hibernate maps them.

-- ─── VMS ─────────────────────────────────────────────────────────────────────

ALTER TABLE manacommunity.vms_vendors
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.vms_bookings
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- vms_purchase_orders already has created_by as a FK (from @JoinColumn); add updated_by.
ALTER TABLE manacommunity.vms_purchase_orders
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- vms_work_orders already has created_by as a FK; add updated_by.
ALTER TABLE manacommunity.vms_work_orders
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ─── Booking / Resource ───────────────────────────────────────────────────────

-- resource already has created_by as a FK; add updated_by.
ALTER TABLE manacommunity.resource
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE manacommunity.resource_booking
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ─── Core model ──────────────────────────────────────────────────────────────

-- community had only created_at; BaseAuditEntity also provides updated_at.
ALTER TABLE manacommunity.community
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP;

-- ─── Helpdesk ─────────────────────────────────────────────────────────────────

ALTER TABLE manacommunity.helpdesk_ticket
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;
