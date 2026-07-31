# Community Finance & Billing Operating System (CFBOS) — Design Specification

## 1. Overview

### 1.1 Purpose

CFBOS is a complete ERP-grade Finance & Billing Platform for the Mana Community Super App. It manages billing, invoicing, accounting, collections, vendor payments, budgeting, funds, taxation, treasury, audit, compliance, reporting, and financial analytics for residential communities.

### 1.2 Key Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Architecture | Monolithic (Spring Boot) | Existing codebase is monolithic; microservice extraction planned later |
| Existing code | Keep both finance subsystems alongside | Zero disruption to working code |
| Geography | India-only, INR, Indian GST/TDS | Current market; no multi-currency overhead |
| Multi-tenancy | Schema-per-tenant | Matches existing Hibernate schema-based tenancy |
| Module structure | Vertical domain packages + thin shared kernel | Clean boundaries, future microservice extraction |

### 1.3 Scope

28 modules, 216 database tables, 14 business engines, ~180 REST endpoints, 7 frontend portals. This spec covers the full blueprint; implementation will be phased.

### 1.4 What CFBOS Is NOT

- Not a replacement for the existing legacy billing or ledger finance modules (they stay untouched)
- Not a standalone microservice (it lives in the existing Spring Boot app)
- Not multi-currency or multi-country (India/INR only)

---

## 2. Architecture

### 2.1 Package Structure

```
com.manacommunity.api.cfbos/
├── shared/                          ← Thin shared kernel
│   ├── entity/                      ← BaseEntity, AuditableEntity, Money, TaxLine
│   ├── enums/                       ← Shared enums (Currency=INR, Status, ApprovalState)
│   ├── exception/                   ← CFBOS domain exceptions
│   ├── event/                       ← Domain event interfaces
│   ├── audit/                       ← Audit trail interceptor
│   ├── approval/                    ← Approval workflow engine (reusable)
│   ├── sequence/                    ← Document number generator (INV-2026-0001)
│   └── specification/              ← Shared JPA Specification builders
│
├── billing/                         ← Billing + Rules Engine
├── invoice/                         ← Invoice Management
├── charge/                          ← Charge Calculation Engine
├── payment/                         ← Payment Platform
├── wallet/                          ← Resident Wallet
├── penalty/                         ← Penalty & Interest Engine
├── accounting/                      ← Accounting Engine
├── expense/                         ← Expense Management
├── vendor/                          ← Vendor Finance
├── fund/                            ← Fund Management
├── budget/                          ← Budget Management
├── treasury/                        ← Treasury & Banking
├── tax/                             ← Tax Engine (GST, TDS)
├── reporting/                       ← Financial Reporting
├── residentportal/                  ← Resident Finance Portal
├── committeportal/                  ← Committee Finance Portal
├── approval/                        ← Approval Workflow
├── ai/                              ← AI Finance Assistant
├── analytics/                       ← Analytics Engine
├── security/                        ← Security (audit, maker-checker)
├── integration/                     ← Integration adapters
├── automation/                      ← Scheduling & automation
└── revenue/                         ← Revenue/subscription model
```

Each module is a self-contained vertical package:
```
cfbos/{module}/
  ├── controller/     ← REST controllers
  ├── dto/            ← Request/Response DTOs
  ├── entity/         ← JPA entities
  ├── enums/          ← Module-specific enums
  ├── engine/         ← Business engine interfaces + implementations
  ├── event/          ← Domain events
  ├── listener/       ← Event listeners
  ├── repository/     ← Spring Data JPA repositories
  ├── service/        ← Application services
  └── specification/  ← JPA Specification builders
```

### 2.2 Module Dependency Map

```
Layer 0 (Foundation):     shared
Layer 1 (Core Engines):   tax → shared
                          accounting → shared, tax
                          charge → shared
                          approval → shared
Layer 2 (Business):       billing → shared, charge, tax, accounting
                          invoice → shared, billing, tax, accounting
                          penalty → shared, billing
                          wallet → shared, accounting
                          payment → shared, wallet, invoice, accounting
Layer 3 (Operations):     expense → shared, accounting, tax, approval
                          vendor → shared, expense, accounting, tax, approval
                          fund → shared, accounting, approval
                          budget → shared, accounting, approval
                          treasury → shared, accounting
Layer 4 (Portals/AI):     residentportal → invoice, payment, wallet, penalty
                          committeeportal → billing, expense, budget, fund, treasury
                          reporting → accounting, billing, expense, vendor, fund, budget
                          analytics → reporting
                          ai → reporting, analytics, billing, payment
                          automation → billing, invoice, penalty, payment, treasury
Layer 5 (Cross-cutting):  security → shared (interceptors, filters)
                          integration → all modules (adapter pattern)
                          revenue → payment, billing
```

### 2.3 Build Order (Phased Delivery)

| Phase | Modules | Outcome |
|---|---|---|
| Phase 1 | shared, tax, accounting, charge | Foundation engines — chart of accounts, journal entries, GST/TDS calc |
| Phase 2 | billing, invoice, penalty | Generate bills, produce invoices, calculate late fees |
| Phase 3 | payment, wallet | Accept payments, manage resident wallet/advances |
| Phase 4 | expense, vendor, approval | Track outflows, vendor lifecycle, approval workflows |
| Phase 5 | fund, budget, treasury | Fund accounting, budgets, bank reconciliation |
| Phase 6 | reporting, analytics, ai | Financial statements, dashboards, AI assistant |
| Phase 7 | portals, automation, security, integration, revenue | User-facing portals, scheduled jobs, hardening |

### 2.4 Cross-Module Communication

All engines communicate via Spring ApplicationEvents — no direct cross-engine method calls for side effects:

```
BillingRunCompletedEvent     → InvoiceEngine (generate invoices)
InvoiceCreatedEvent          → AccountingEngine (journal entry)
                             → NotificationEngine (send bill email/SMS)
InvoiceOverdueEvent          → PenaltyEngine (calculate late fee)
                             → NotificationEngine (reminder)
PaymentReceivedEvent         → AccountingEngine (journal entry)
                             → WalletEngine (credit if overpayment)
                             → NotificationEngine (receipt)
ExpenseApprovedEvent         → AccountingEngine (journal entry)
                             → BudgetEngine (check availability)
VendorPaymentEvent           → AccountingEngine (journal entry)
                             → TaxEngine (TDS deduction)
FundTransferEvent            → AccountingEngine (journal entries for both funds)
PenaltyAppliedEvent          → InvoiceEngine (add penalty line to next invoice)
BudgetThresholdBreachedEvent → NotificationEngine (alert committee)
```

---

## 3. Database Schema

### 3.1 Design Conventions

| Convention | Rule |
|---|---|
| Table prefix | `cfbos_` |
| Naming | `snake_case`, singular nouns |
| Primary keys | `id BIGSERIAL PRIMARY KEY` |
| Tenant isolation | Schema-per-tenant, no `community_id` column |
| Audit columns | `created_by`, `created_at`, `updated_by`, `updated_at` on every table |
| Soft delete | `is_deleted BOOLEAN DEFAULT FALSE`, `deleted_at TIMESTAMP` |
| Versioning | `version INTEGER DEFAULT 0` (optimistic locking) |
| Money | `NUMERIC(18,2)` — INR with paise precision |
| Status columns | PostgreSQL ENUMs per domain |
| History tables | `_history` suffix, same columns + `revision`, `change_type`, `changed_at` |
| Indexes | On all FKs, status columns, date ranges, composite lookups |

### 3.2 Complete Table Catalog (216 tables)

#### 3.2.1 Shared Kernel (10 tables)

**cfbos_audit_log**
```sql
id BIGSERIAL PRIMARY KEY,
event_type VARCHAR(100) NOT NULL,
entity_type VARCHAR(100) NOT NULL,
entity_id BIGINT NOT NULL,
action VARCHAR(50) NOT NULL,
actor_id BIGINT,
actor_role VARCHAR(50),
actor_ip VARCHAR(45),
before_state JSONB,
after_state JSONB,
event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
checksum VARCHAR(64) NOT NULL
```

**cfbos_document_sequence**
```sql
id BIGSERIAL PRIMARY KEY,
document_type VARCHAR(50) NOT NULL UNIQUE,
prefix VARCHAR(20) NOT NULL,
fiscal_year VARCHAR(9) NOT NULL,
current_value BIGINT NOT NULL DEFAULT 0,
padding_length INTEGER NOT NULL DEFAULT 6,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_approval_workflow**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
entity_type VARCHAR(100) NOT NULL,
description TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_approval_step**
```sql
id BIGSERIAL PRIMARY KEY,
workflow_id BIGINT NOT NULL REFERENCES cfbos_approval_workflow(id),
step_order INTEGER NOT NULL,
approver_role VARCHAR(50) NOT NULL,
min_amount NUMERIC(18,2),
max_amount NUMERIC(18,2),
is_auto_approve BOOLEAN NOT NULL DEFAULT FALSE,
auto_approve_below NUMERIC(18,2),
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_approval_request**
```sql
id BIGSERIAL PRIMARY KEY,
workflow_id BIGINT NOT NULL REFERENCES cfbos_approval_workflow(id),
entity_type VARCHAR(100) NOT NULL,
entity_id BIGINT NOT NULL,
current_step INTEGER NOT NULL DEFAULT 1,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
submitted_by BIGINT NOT NULL,
submitted_at TIMESTAMPTZ NOT NULL,
amount NUMERIC(18,2),
remarks TEXT,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_approval_action**
```sql
id BIGSERIAL PRIMARY KEY,
request_id BIGINT NOT NULL REFERENCES cfbos_approval_request(id),
step_order INTEGER NOT NULL,
action VARCHAR(20) NOT NULL,
actor_id BIGINT NOT NULL,
acted_at TIMESTAMPTZ NOT NULL,
comments TEXT
```

**cfbos_attachment**
```sql
id BIGSERIAL PRIMARY KEY,
entity_type VARCHAR(100) NOT NULL,
entity_id BIGINT NOT NULL,
file_name VARCHAR(255) NOT NULL,
file_type VARCHAR(50),
file_size BIGINT,
storage_path VARCHAR(500) NOT NULL,
uploaded_by BIGINT NOT NULL,
uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_note**
```sql
id BIGSERIAL PRIMARY KEY,
entity_type VARCHAR(100) NOT NULL,
entity_id BIGINT NOT NULL,
content TEXT NOT NULL,
created_by BIGINT NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_notification_log**
```sql
id BIGSERIAL PRIMARY KEY,
notification_type VARCHAR(50) NOT NULL,
channel VARCHAR(20) NOT NULL,
recipient_id BIGINT,
recipient_contact VARCHAR(255),
subject VARCHAR(255),
body TEXT,
entity_type VARCHAR(100),
entity_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
sent_at TIMESTAMPTZ,
error_message TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_config**
```sql
id BIGSERIAL PRIMARY KEY,
config_key VARCHAR(100) NOT NULL UNIQUE,
config_value TEXT NOT NULL,
config_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
description VARCHAR(255),
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.2 Billing & Rules Engine (14 tables)

**cfbos_charge_head**
```sql
id BIGSERIAL PRIMARY KEY,
code VARCHAR(20) NOT NULL UNIQUE,
name VARCHAR(100) NOT NULL,
description TEXT,
display_order INTEGER NOT NULL DEFAULT 0,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_charge_type**
```sql
id BIGSERIAL PRIMARY KEY,
charge_head_id BIGINT NOT NULL REFERENCES cfbos_charge_head(id),
code VARCHAR(20) NOT NULL UNIQUE,
name VARCHAR(100) NOT NULL,
description TEXT,
default_hsn_sac_code VARCHAR(10),
is_taxable BOOLEAN NOT NULL DEFAULT TRUE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_billing_category**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
code VARCHAR(20) NOT NULL UNIQUE,
description TEXT,
category_type VARCHAR(30) NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
display_order INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_billing_schedule**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
frequency VARCHAR(20) NOT NULL,
billing_day INTEGER NOT NULL DEFAULT 1,
due_day_offset INTEGER NOT NULL DEFAULT 15,
advance_billing_days INTEGER NOT NULL DEFAULT 0,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_billing_rule**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(150) NOT NULL,
billing_category_id BIGINT NOT NULL REFERENCES cfbos_billing_category(id),
charge_type_id BIGINT NOT NULL REFERENCES cfbos_charge_type(id),
billing_schedule_id BIGINT NOT NULL REFERENCES cfbos_billing_schedule(id),
calculation_method VARCHAR(30) NOT NULL,
fixed_amount NUMERIC(18,2),
rate_per_unit NUMERIC(18,4),
formula_id BIGINT,
slab_config_id BIGINT,
applicable_property_types TEXT[],
is_taxable BOOLEAN NOT NULL DEFAULT TRUE,
tax_rate_id BIGINT,
hsn_sac_code_id BIGINT,
effective_from DATE NOT NULL,
effective_to DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
priority INTEGER NOT NULL DEFAULT 0,
version INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_billing_rule_condition**
```sql
id BIGSERIAL PRIMARY KEY,
billing_rule_id BIGINT NOT NULL REFERENCES cfbos_billing_rule(id),
field_name VARCHAR(50) NOT NULL,
operator VARCHAR(20) NOT NULL,
field_value VARCHAR(255) NOT NULL,
logical_group INTEGER NOT NULL DEFAULT 0,
created_at TIMESTAMPTZ
```

**cfbos_billing_rule_formula**
```sql
id BIGSERIAL PRIMARY KEY,
billing_rule_id BIGINT NOT NULL REFERENCES cfbos_billing_rule(id),
expression TEXT NOT NULL,
description TEXT,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_billing_slab**
```sql
id BIGSERIAL PRIMARY KEY,
billing_rule_id BIGINT NOT NULL REFERENCES cfbos_billing_rule(id),
slab_from NUMERIC(18,4) NOT NULL,
slab_to NUMERIC(18,4),
rate NUMERIC(18,4) NOT NULL,
fixed_charge NUMERIC(18,2) NOT NULL DEFAULT 0,
slab_order INTEGER NOT NULL,
created_at TIMESTAMPTZ
```

**cfbos_rate_card**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
description TEXT,
effective_from DATE NOT NULL,
effective_to DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_rate_card_line**
```sql
id BIGSERIAL PRIMARY KEY,
rate_card_id BIGINT NOT NULL REFERENCES cfbos_rate_card(id),
charge_type_id BIGINT NOT NULL REFERENCES cfbos_charge_type(id),
property_type VARCHAR(30),
rate NUMERIC(18,4) NOT NULL,
unit VARCHAR(20),
created_at TIMESTAMPTZ
```

**cfbos_billing_run**
```sql
id BIGSERIAL PRIMARY KEY,
run_number VARCHAR(30) NOT NULL UNIQUE,
billing_period_start DATE NOT NULL,
billing_period_end DATE NOT NULL,
billing_schedule_id BIGINT REFERENCES cfbos_billing_schedule(id),
run_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
total_properties INTEGER NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
auto_send BOOLEAN NOT NULL DEFAULT FALSE,
executed_by BIGINT, executed_at TIMESTAMPTZ,
error_log TEXT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_billing_run_line**
```sql
id BIGSERIAL PRIMARY KEY,
billing_run_id BIGINT NOT NULL REFERENCES cfbos_billing_run(id),
property_id BIGINT NOT NULL,
resident_id BIGINT NOT NULL,
billing_rule_id BIGINT NOT NULL REFERENCES cfbos_billing_rule(id),
charge_type_id BIGINT NOT NULL REFERENCES cfbos_charge_type(id),
description VARCHAR(255),
quantity NUMERIC(18,4) NOT NULL DEFAULT 1,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
calculation_details JSONB,
created_at TIMESTAMPTZ
```

**cfbos_billing_exception**
```sql
id BIGSERIAL PRIMARY KEY,
property_id BIGINT NOT NULL,
billing_rule_id BIGINT NOT NULL REFERENCES cfbos_billing_rule(id),
exception_type VARCHAR(20) NOT NULL,
override_amount NUMERIC(18,2),
override_rate NUMERIC(18,4),
reason TEXT NOT NULL,
effective_from DATE NOT NULL,
effective_to DATE,
approved_by BIGINT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_property_billing_config**
```sql
id BIGSERIAL PRIMARY KEY,
property_id BIGINT NOT NULL UNIQUE,
billing_profile VARCHAR(50),
is_billing_active BOOLEAN NOT NULL DEFAULT TRUE,
custom_due_day_offset INTEGER,
custom_grace_period INTEGER,
notes TEXT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.3 Invoice Management (14 tables)

**cfbos_invoice**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_number VARCHAR(30) NOT NULL UNIQUE,
invoice_date DATE NOT NULL,
due_date DATE NOT NULL,
property_id BIGINT NOT NULL,
resident_id BIGINT NOT NULL,
billing_run_id BIGINT REFERENCES cfbos_billing_run(id),
invoice_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
amount_paid NUMERIC(18,2) NOT NULL DEFAULT 0,
balance_due NUMERIC(18,2) NOT NULL DEFAULT 0,
billing_period_start DATE,
billing_period_end DATE,
notes TEXT,
terms TEXT,
is_gst_invoice BOOLEAN NOT NULL DEFAULT FALSE,
community_gstin VARCHAR(15),
template_id BIGINT,
journal_entry_id BIGINT,
version INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_invoice_line**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
charge_type_id BIGINT REFERENCES cfbos_charge_type(id),
description VARCHAR(255) NOT NULL,
hsn_sac_code VARCHAR(10),
quantity NUMERIC(18,4) NOT NULL DEFAULT 1,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
line_order INTEGER NOT NULL DEFAULT 0,
billing_run_line_id BIGINT,
created_at TIMESTAMPTZ
```

**cfbos_invoice_tax_line**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
tax_type VARCHAR(10) NOT NULL,
tax_rate NUMERIC(5,2) NOT NULL,
taxable_amount NUMERIC(18,2) NOT NULL,
tax_amount NUMERIC(18,2) NOT NULL,
hsn_sac_code VARCHAR(10)
```

**cfbos_credit_note**
```sql
id BIGSERIAL PRIMARY KEY,
credit_note_number VARCHAR(30) NOT NULL UNIQUE,
credit_note_date DATE NOT NULL,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
resident_id BIGINT NOT NULL,
reason TEXT NOT NULL,
subtotal NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_credit_note_line**
```sql
id BIGSERIAL PRIMARY KEY,
credit_note_id BIGINT NOT NULL REFERENCES cfbos_credit_note(id),
invoice_line_id BIGINT REFERENCES cfbos_invoice_line(id),
description VARCHAR(255) NOT NULL,
quantity NUMERIC(18,4) NOT NULL DEFAULT 1,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL
```

**cfbos_debit_note**
```sql
id BIGSERIAL PRIMARY KEY,
debit_note_number VARCHAR(30) NOT NULL UNIQUE,
debit_note_date DATE NOT NULL,
invoice_id BIGINT REFERENCES cfbos_invoice(id),
resident_id BIGINT NOT NULL,
reason TEXT NOT NULL,
subtotal NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_debit_note_line**
```sql
id BIGSERIAL PRIMARY KEY,
debit_note_id BIGINT NOT NULL REFERENCES cfbos_debit_note(id),
description VARCHAR(255) NOT NULL,
quantity NUMERIC(18,4) NOT NULL DEFAULT 1,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL
```

**cfbos_invoice_template**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
description TEXT,
template_content TEXT NOT NULL,
is_default BOOLEAN NOT NULL DEFAULT FALSE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_invoice_schedule**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
billing_rule_ids BIGINT[] NOT NULL,
frequency VARCHAR(20) NOT NULL,
next_run_date DATE NOT NULL,
last_run_date DATE,
auto_send BOOLEAN NOT NULL DEFAULT FALSE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_invoice_adjustment**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
adjustment_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
adjusted_by BIGINT NOT NULL,
adjusted_at TIMESTAMPTZ NOT NULL,
approval_request_id BIGINT
```

**cfbos_invoice_history**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL,
revision INTEGER NOT NULL,
change_type VARCHAR(20) NOT NULL,
snapshot JSONB NOT NULL,
changed_by BIGINT NOT NULL,
changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_recurring_invoice**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
resident_id BIGINT NOT NULL,
property_id BIGINT NOT NULL,
template_data JSONB NOT NULL,
frequency VARCHAR(20) NOT NULL,
start_date DATE NOT NULL,
end_date DATE,
next_generation_date DATE NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_recurring_invoice_log**
```sql
id BIGSERIAL PRIMARY KEY,
recurring_invoice_id BIGINT NOT NULL REFERENCES cfbos_recurring_invoice(id),
invoice_id BIGINT REFERENCES cfbos_invoice(id),
generation_date DATE NOT NULL,
status VARCHAR(20) NOT NULL,
error_message TEXT,
created_at TIMESTAMPTZ
```

**cfbos_demand_note**
```sql
id BIGSERIAL PRIMARY KEY,
demand_note_number VARCHAR(30) NOT NULL UNIQUE,
property_id BIGINT NOT NULL,
resident_id BIGINT NOT NULL,
demand_date DATE NOT NULL,
due_date DATE NOT NULL,
description TEXT NOT NULL,
total_amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
invoice_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.4 Charge Calculation Engine (9 tables)

**cfbos_formula**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
expression TEXT NOT NULL,
description TEXT,
result_type VARCHAR(20) NOT NULL DEFAULT 'AMOUNT',
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_formula_variable**
```sql
id BIGSERIAL PRIMARY KEY,
formula_id BIGINT NOT NULL REFERENCES cfbos_formula(id),
variable_name VARCHAR(50) NOT NULL,
variable_source VARCHAR(50) NOT NULL,
source_field VARCHAR(100) NOT NULL,
default_value VARCHAR(50),
description VARCHAR(255)
```

**cfbos_slab_config**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
description TEXT,
unit_label VARCHAR(30),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_tier_config**
```sql
id BIGSERIAL PRIMARY KEY,
slab_config_id BIGINT NOT NULL REFERENCES cfbos_slab_config(id),
tier_from NUMERIC(18,4) NOT NULL,
tier_to NUMERIC(18,4),
rate NUMERIC(18,4) NOT NULL,
fixed_charge NUMERIC(18,2) NOT NULL DEFAULT 0,
tier_order INTEGER NOT NULL
```

**cfbos_waiver**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT,
property_id BIGINT,
charge_type_id BIGINT REFERENCES cfbos_charge_type(id),
waiver_type VARCHAR(20) NOT NULL,
waiver_amount NUMERIC(18,2),
waiver_percentage NUMERIC(5,2),
reason TEXT NOT NULL,
effective_from DATE NOT NULL,
effective_to DATE,
approval_request_id BIGINT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_exemption**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT,
property_id BIGINT,
billing_rule_id BIGINT REFERENCES cfbos_billing_rule(id),
reason TEXT NOT NULL,
effective_from DATE NOT NULL,
effective_to DATE,
approval_request_id BIGINT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_subsidy**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
charge_type_id BIGINT REFERENCES cfbos_charge_type(id),
subsidy_type VARCHAR(20) NOT NULL,
subsidy_amount NUMERIC(18,2),
subsidy_percentage NUMERIC(5,2),
eligibility_criteria JSONB,
effective_from DATE NOT NULL,
effective_to DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_meter**
```sql
id BIGSERIAL PRIMARY KEY,
property_id BIGINT NOT NULL,
meter_type VARCHAR(20) NOT NULL,
meter_number VARCHAR(50) NOT NULL,
installation_date DATE,
last_reading_date DATE,
last_reading_value NUMERIC(18,4),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_meter_reading**
```sql
id BIGSERIAL PRIMARY KEY,
meter_id BIGINT NOT NULL REFERENCES cfbos_meter(id),
reading_date DATE NOT NULL,
reading_value NUMERIC(18,4) NOT NULL,
previous_reading NUMERIC(18,4),
consumption NUMERIC(18,4),
reading_type VARCHAR(20) NOT NULL DEFAULT 'ACTUAL',
photo_path VARCHAR(500),
recorded_by BIGINT NOT NULL,
created_at TIMESTAMPTZ
```

#### 3.2.5 Payment Platform (16 tables)

**cfbos_payment**
```sql
id BIGSERIAL PRIMARY KEY,
payment_number VARCHAR(30) NOT NULL UNIQUE,
payment_date DATE NOT NULL,
resident_id BIGINT NOT NULL,
property_id BIGINT NOT NULL,
payment_method VARCHAR(20) NOT NULL,
payment_mode VARCHAR(10) NOT NULL DEFAULT 'ONLINE',
amount NUMERIC(18,2) NOT NULL,
applied_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
unapplied_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
gateway_txn_id BIGINT,
gateway_reference VARCHAR(100),
status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
receipt_id BIGINT,
journal_entry_id BIGINT,
remarks TEXT,
version INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_payment_line**
```sql
id BIGSERIAL PRIMARY KEY,
payment_id BIGINT NOT NULL REFERENCES cfbos_payment(id),
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
allocated_amount NUMERIC(18,2) NOT NULL,
allocation_date DATE NOT NULL,
created_at TIMESTAMPTZ
```

**cfbos_payment_method**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(50) NOT NULL,
method_type VARCHAR(20) NOT NULL,
is_online BOOLEAN NOT NULL DEFAULT TRUE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
display_order INTEGER NOT NULL DEFAULT 0,
icon_name VARCHAR(50),
created_at TIMESTAMPTZ
```

**cfbos_payment_gateway_config**
```sql
id BIGSERIAL PRIMARY KEY,
gateway_name VARCHAR(50) NOT NULL,
api_key_encrypted VARCHAR(500),
api_secret_encrypted VARCHAR(500),
webhook_secret_encrypted VARCHAR(500),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
is_test_mode BOOLEAN NOT NULL DEFAULT FALSE,
config_json JSONB,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_payment_gateway_txn**
```sql
id BIGSERIAL PRIMARY KEY,
gateway_name VARCHAR(50) NOT NULL,
gateway_order_id VARCHAR(100),
gateway_payment_id VARCHAR(100),
gateway_signature VARCHAR(255),
amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL,
payment_method VARCHAR(30),
response_json JSONB,
error_code VARCHAR(50),
error_message TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ
```

**cfbos_payment_gateway_settlement**
```sql
id BIGSERIAL PRIMARY KEY,
gateway_name VARCHAR(50) NOT NULL,
settlement_id VARCHAR(100) NOT NULL,
settlement_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
fees NUMERIC(18,2) NOT NULL DEFAULT 0,
tax_on_fees NUMERIC(18,2) NOT NULL DEFAULT 0,
net_amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL,
utr VARCHAR(100),
created_at TIMESTAMPTZ
```

**cfbos_receipt**
```sql
id BIGSERIAL PRIMARY KEY,
receipt_number VARCHAR(30) NOT NULL UNIQUE,
receipt_date DATE NOT NULL,
payment_id BIGINT NOT NULL REFERENCES cfbos_payment(id),
resident_id BIGINT NOT NULL,
amount NUMERIC(18,2) NOT NULL,
payment_method VARCHAR(20) NOT NULL,
narration TEXT,
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_refund**
```sql
id BIGSERIAL PRIMARY KEY,
refund_number VARCHAR(30) NOT NULL UNIQUE,
refund_date DATE NOT NULL,
payment_id BIGINT NOT NULL REFERENCES cfbos_payment(id),
resident_id BIGINT NOT NULL,
amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
refund_method VARCHAR(20) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
approval_request_id BIGINT,
gateway_refund_id VARCHAR(100),
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_refund_line**
```sql
id BIGSERIAL PRIMARY KEY,
refund_id BIGINT NOT NULL REFERENCES cfbos_refund(id),
invoice_id BIGINT REFERENCES cfbos_invoice(id),
amount NUMERIC(18,2) NOT NULL,
description VARCHAR(255)
```

**cfbos_payment_reminder**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
resident_id BIGINT NOT NULL,
reminder_type VARCHAR(20) NOT NULL,
scheduled_date DATE NOT NULL,
sent_date DATE,
channel VARCHAR(20) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
notification_log_id BIGINT,
created_at TIMESTAMPTZ
```

**cfbos_auto_debit_mandate**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
payment_method VARCHAR(20) NOT NULL,
mandate_reference VARCHAR(100),
max_amount NUMERIC(18,2) NOT NULL,
frequency VARCHAR(20) NOT NULL,
start_date DATE NOT NULL,
end_date DATE,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
gateway_mandate_id VARCHAR(100),
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_payment_plan**
```sql
id BIGSERIAL PRIMARY KEY,
plan_number VARCHAR(30) NOT NULL UNIQUE,
resident_id BIGINT NOT NULL,
total_amount NUMERIC(18,2) NOT NULL,
num_installments INTEGER NOT NULL,
installment_amount NUMERIC(18,2) NOT NULL,
start_date DATE NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
invoice_ids BIGINT[],
approval_request_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_payment_plan_installment**
```sql
id BIGSERIAL PRIMARY KEY,
payment_plan_id BIGINT NOT NULL REFERENCES cfbos_payment_plan(id),
installment_number INTEGER NOT NULL,
due_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
payment_id BIGINT,
paid_date DATE
```

**cfbos_payment_split**
```sql
id BIGSERIAL PRIMARY KEY,
payment_id BIGINT NOT NULL REFERENCES cfbos_payment(id),
split_method VARCHAR(20) NOT NULL,
split_amount NUMERIC(18,2) NOT NULL,
source_type VARCHAR(20) NOT NULL,
source_reference VARCHAR(100),
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_at TIMESTAMPTZ
```

**cfbos_advance_payment**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
payment_id BIGINT NOT NULL REFERENCES cfbos_payment(id),
amount NUMERIC(18,2) NOT NULL,
utilized_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
balance NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_advance_adjustment**
```sql
id BIGSERIAL PRIMARY KEY,
advance_payment_id BIGINT NOT NULL REFERENCES cfbos_advance_payment(id),
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
amount NUMERIC(18,2) NOT NULL,
adjusted_date DATE NOT NULL,
created_at TIMESTAMPTZ
```

#### 3.2.6 Resident Wallet (6 tables)

**cfbos_wallet**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL UNIQUE,
balance NUMERIC(18,2) NOT NULL DEFAULT 0,
total_credited NUMERIC(18,2) NOT NULL DEFAULT 0,
total_debited NUMERIC(18,2) NOT NULL DEFAULT 0,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
version INTEGER NOT NULL DEFAULT 0,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_wallet_transaction**
```sql
id BIGSERIAL PRIMARY KEY,
wallet_id BIGINT NOT NULL REFERENCES cfbos_wallet(id),
transaction_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
balance_after NUMERIC(18,2) NOT NULL,
reference_type VARCHAR(50),
reference_id BIGINT,
narration VARCHAR(255),
created_by BIGINT, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_wallet_adjustment**
```sql
id BIGSERIAL PRIMARY KEY,
wallet_id BIGINT NOT NULL REFERENCES cfbos_wallet(id),
adjustment_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
approval_request_id BIGINT,
adjusted_by BIGINT NOT NULL,
adjusted_at TIMESTAMPTZ NOT NULL
```

**cfbos_security_deposit**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
property_id BIGINT NOT NULL,
deposit_type VARCHAR(30) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
deposit_date DATE NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'HELD',
refund_date DATE,
refund_amount NUMERIC(18,2),
deductions NUMERIC(18,2) NOT NULL DEFAULT 0,
deduction_reason TEXT,
receipt_id BIGINT,
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_reward_point**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL UNIQUE,
total_points INTEGER NOT NULL DEFAULT 0,
redeemed_points INTEGER NOT NULL DEFAULT 0,
available_points INTEGER NOT NULL DEFAULT 0,
updated_at TIMESTAMPTZ
```

**cfbos_reward_point_txn**
```sql
id BIGSERIAL PRIMARY KEY,
reward_point_id BIGINT NOT NULL REFERENCES cfbos_reward_point(id),
transaction_type VARCHAR(20) NOT NULL,
points INTEGER NOT NULL,
reference_type VARCHAR(50),
reference_id BIGINT,
description VARCHAR(255),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

#### 3.2.7 Penalty & Interest Engine (7 tables)

**cfbos_penalty_config**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
grace_period_days INTEGER NOT NULL DEFAULT 15,
late_fee_type VARCHAR(20) NOT NULL,
late_fee_amount NUMERIC(18,2),
late_fee_percentage NUMERIC(5,2),
interest_type VARCHAR(20),
interest_rate_annual NUMERIC(5,2),
interest_frequency VARCHAR(20),
compound_frequency VARCHAR(20),
max_penalty_cap NUMERIC(18,2),
auto_apply BOOLEAN NOT NULL DEFAULT TRUE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_penalty_rule**
```sql
id BIGSERIAL PRIMARY KEY,
penalty_config_id BIGINT NOT NULL REFERENCES cfbos_penalty_config(id),
days_overdue_from INTEGER NOT NULL,
days_overdue_to INTEGER,
penalty_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2),
percentage NUMERIC(5,2),
is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
recurrence_interval_days INTEGER
```

**cfbos_penalty**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
resident_id BIGINT NOT NULL,
penalty_config_id BIGINT NOT NULL REFERENCES cfbos_penalty_config(id),
penalty_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
calculated_date DATE NOT NULL,
applied_to_invoice_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'CALCULATED',
journal_entry_id BIGINT,
created_at TIMESTAMPTZ
```

**cfbos_interest_config**
```sql
id BIGSERIAL PRIMARY KEY,
interest_type VARCHAR(20) NOT NULL,
annual_rate NUMERIC(5,2) NOT NULL,
calculation_frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
compound_frequency VARCHAR(20),
minimum_days INTEGER NOT NULL DEFAULT 1,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_interest_accrual**
```sql
id BIGSERIAL PRIMARY KEY,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
resident_id BIGINT NOT NULL,
principal_amount NUMERIC(18,2) NOT NULL,
interest_amount NUMERIC(18,2) NOT NULL,
accrual_from DATE NOT NULL,
accrual_to DATE NOT NULL,
days INTEGER NOT NULL,
rate_applied NUMERIC(5,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ACCRUED',
journal_entry_id BIGINT,
created_at TIMESTAMPTZ
```

**cfbos_grace_period_config**
```sql
id BIGSERIAL PRIMARY KEY,
billing_category_id BIGINT REFERENCES cfbos_billing_category(id),
grace_period_days INTEGER NOT NULL,
applies_to_property_types TEXT[],
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_penalty_waiver**
```sql
id BIGSERIAL PRIMARY KEY,
penalty_id BIGINT NOT NULL REFERENCES cfbos_penalty(id),
waiver_amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
approval_request_id BIGINT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
waived_by BIGINT,
waived_at TIMESTAMPTZ,
created_at TIMESTAMPTZ
```

#### 3.2.8 Accounting Engine (14 tables)

**cfbos_fiscal_year**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(20) NOT NULL UNIQUE,
start_date DATE NOT NULL,
end_date DATE NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
is_current BOOLEAN NOT NULL DEFAULT FALSE,
closed_by BIGINT, closed_at TIMESTAMPTZ,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_accounting_period**
```sql
id BIGSERIAL PRIMARY KEY,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
name VARCHAR(50) NOT NULL,
start_date DATE NOT NULL,
end_date DATE NOT NULL,
period_number INTEGER NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
closed_by BIGINT, closed_at TIMESTAMPTZ,
created_at TIMESTAMPTZ
```

**cfbos_account_group**
```sql
id BIGSERIAL PRIMARY KEY,
code VARCHAR(10) NOT NULL UNIQUE,
name VARCHAR(100) NOT NULL,
account_type VARCHAR(20) NOT NULL,
parent_group_id BIGINT REFERENCES cfbos_account_group(id),
display_order INTEGER NOT NULL DEFAULT 0,
is_system BOOLEAN NOT NULL DEFAULT FALSE,
created_at TIMESTAMPTZ
```

**cfbos_account**
```sql
id BIGSERIAL PRIMARY KEY,
code VARCHAR(20) NOT NULL UNIQUE,
name VARCHAR(150) NOT NULL,
account_group_id BIGINT NOT NULL REFERENCES cfbos_account_group(id),
parent_account_id BIGINT REFERENCES cfbos_account(id),
account_type VARCHAR(20) NOT NULL,
is_system_account BOOLEAN NOT NULL DEFAULT FALSE,
is_bank_account BOOLEAN NOT NULL DEFAULT FALSE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
opening_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
description TEXT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_journal_entry**
```sql
id BIGSERIAL PRIMARY KEY,
entry_number VARCHAR(30) NOT NULL UNIQUE,
entry_date DATE NOT NULL,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
accounting_period_id BIGINT NOT NULL REFERENCES cfbos_accounting_period(id),
entry_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
source_module VARCHAR(30),
source_document_type VARCHAR(50),
source_document_id BIGINT,
narration TEXT NOT NULL,
total_debit NUMERIC(18,2) NOT NULL,
total_credit NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
posted_by BIGINT, posted_at TIMESTAMPTZ,
reversed_by BIGINT, reversed_at TIMESTAMPTZ,
reversal_of_id BIGINT,
version INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_journal_line**
```sql
id BIGSERIAL PRIMARY KEY,
journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id),
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
cost_center_id BIGINT,
fund_id BIGINT,
debit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
narration VARCHAR(255),
CONSTRAINT chk_single_side CHECK (
    (debit_amount > 0 AND credit_amount = 0) OR
    (credit_amount > 0 AND debit_amount = 0)
)
```

**cfbos_ledger**
```sql
id BIGSERIAL PRIMARY KEY,
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id),
journal_line_id BIGINT NOT NULL REFERENCES cfbos_journal_line(id),
posting_date DATE NOT NULL,
debit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
balance NUMERIC(18,2) NOT NULL,
narration VARCHAR(255),
source_module VARCHAR(30),
source_document_type VARCHAR(50),
source_document_id BIGINT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_sub_ledger**
```sql
id BIGSERIAL PRIMARY KEY,
ledger_type VARCHAR(20) NOT NULL,
party_id BIGINT NOT NULL,
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id),
posting_date DATE NOT NULL,
debit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
balance NUMERIC(18,2) NOT NULL,
narration VARCHAR(255),
source_document_type VARCHAR(50),
source_document_id BIGINT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_opening_balance**
```sql
id BIGSERIAL PRIMARY KEY,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
debit_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
credit_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_closing_entry**
```sql
id BIGSERIAL PRIMARY KEY,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
closing_type VARCHAR(30) NOT NULL,
journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id),
surplus_deficit NUMERIC(18,2),
closed_by BIGINT NOT NULL,
closed_at TIMESTAMPTZ NOT NULL
```

**cfbos_cost_center**
```sql
id BIGSERIAL PRIMARY KEY,
code VARCHAR(20) NOT NULL UNIQUE,
name VARCHAR(100) NOT NULL,
parent_id BIGINT REFERENCES cfbos_cost_center(id),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_accounting_config**
```sql
id BIGSERIAL PRIMARY KEY,
config_key VARCHAR(100) NOT NULL UNIQUE,
config_value TEXT NOT NULL,
description VARCHAR(255),
updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_contra_entry**
```sql
id BIGSERIAL PRIMARY KEY,
entry_date DATE NOT NULL,
from_account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
to_account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
amount NUMERIC(18,2) NOT NULL,
narration TEXT,
journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id),
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_day_book**
```sql
id BIGSERIAL PRIMARY KEY,
entry_date DATE NOT NULL,
journal_entry_id BIGINT NOT NULL REFERENCES cfbos_journal_entry(id),
narration TEXT NOT NULL,
total_debit NUMERIC(18,2) NOT NULL,
total_credit NUMERIC(18,2) NOT NULL,
source_module VARCHAR(30),
source_document_type VARCHAR(50),
source_document_id BIGINT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

#### 3.2.9 Expense Management (6 tables)

**cfbos_expense_category**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
code VARCHAR(20) NOT NULL UNIQUE,
parent_id BIGINT REFERENCES cfbos_expense_category(id),
account_id BIGINT REFERENCES cfbos_account(id),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_expense**
```sql
id BIGSERIAL PRIMARY KEY,
expense_number VARCHAR(30) NOT NULL UNIQUE,
expense_date DATE NOT NULL,
expense_category_id BIGINT NOT NULL REFERENCES cfbos_expense_category(id),
description TEXT NOT NULL,
subtotal NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
payment_mode VARCHAR(20),
payment_reference VARCHAR(100),
vendor_id BIGINT,
is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
approval_request_id BIGINT,
journal_entry_id BIGINT,
budget_line_id BIGINT,
cost_center_id BIGINT,
fund_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_expense_line**
```sql
id BIGSERIAL PRIMARY KEY,
expense_id BIGINT NOT NULL REFERENCES cfbos_expense(id),
description VARCHAR(255) NOT NULL,
account_id BIGINT REFERENCES cfbos_account(id),
quantity NUMERIC(18,4) NOT NULL DEFAULT 1,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
hsn_sac_code VARCHAR(10),
is_taxable BOOLEAN NOT NULL DEFAULT FALSE
```

**cfbos_expense_tax_line**
```sql
id BIGSERIAL PRIMARY KEY,
expense_id BIGINT NOT NULL REFERENCES cfbos_expense(id),
tax_type VARCHAR(10) NOT NULL,
tax_rate NUMERIC(5,2) NOT NULL,
taxable_amount NUMERIC(18,2) NOT NULL,
tax_amount NUMERIC(18,2) NOT NULL
```

**cfbos_recurring_expense**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
expense_category_id BIGINT NOT NULL REFERENCES cfbos_expense_category(id),
template_data JSONB NOT NULL,
frequency VARCHAR(20) NOT NULL,
next_generation_date DATE NOT NULL,
vendor_id BIGINT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_expense_allocation**
```sql
id BIGSERIAL PRIMARY KEY,
expense_id BIGINT NOT NULL REFERENCES cfbos_expense(id),
cost_center_id BIGINT NOT NULL REFERENCES cfbos_cost_center(id),
allocation_percentage NUMERIC(5,2) NOT NULL,
allocated_amount NUMERIC(18,2) NOT NULL
```

#### 3.2.10 Vendor Finance (18 tables)

**cfbos_vendor**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_code VARCHAR(20) NOT NULL UNIQUE,
name VARCHAR(200) NOT NULL,
display_name VARCHAR(100),
vendor_category_id BIGINT,
contact_person VARCHAR(100),
email VARCHAR(255),
phone VARCHAR(20),
address TEXT,
city VARCHAR(100),
state VARCHAR(100),
pincode VARCHAR(10),
gstin VARCHAR(15),
pan VARCHAR(10),
tds_applicable BOOLEAN NOT NULL DEFAULT FALSE,
default_tds_section_id BIGINT,
payment_terms_days INTEGER NOT NULL DEFAULT 30,
account_id BIGINT REFERENCES cfbos_account(id),
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_vendor_category**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
code VARCHAR(20) NOT NULL UNIQUE,
description TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_vendor_bank_account**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
account_holder_name VARCHAR(200) NOT NULL,
account_number_encrypted VARCHAR(500) NOT NULL,
ifsc_code VARCHAR(11) NOT NULL,
bank_name VARCHAR(100) NOT NULL,
branch_name VARCHAR(100),
is_primary BOOLEAN NOT NULL DEFAULT FALSE,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_vendor_contract**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
contract_number VARCHAR(30) NOT NULL UNIQUE,
title VARCHAR(200) NOT NULL,
start_date DATE NOT NULL,
end_date DATE NOT NULL,
contract_value NUMERIC(18,2),
monthly_value NUMERIC(18,2),
terms TEXT,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
document_path VARCHAR(500),
auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_purchase_order**
```sql
id BIGSERIAL PRIMARY KEY,
po_number VARCHAR(30) NOT NULL UNIQUE,
po_date DATE NOT NULL,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
delivery_date DATE,
subtotal NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
approval_request_id BIGINT,
terms TEXT,
notes TEXT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_purchase_order_line**
```sql
id BIGSERIAL PRIMARY KEY,
purchase_order_id BIGINT NOT NULL REFERENCES cfbos_purchase_order(id),
description VARCHAR(255) NOT NULL,
hsn_sac_code VARCHAR(10),
quantity NUMERIC(18,4) NOT NULL,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL
```

**cfbos_vendor_invoice**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_invoice_number VARCHAR(30) NOT NULL UNIQUE,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
vendor_reference VARCHAR(100),
invoice_date DATE NOT NULL,
due_date DATE NOT NULL,
purchase_order_id BIGINT REFERENCES cfbos_purchase_order(id),
subtotal NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
tds_applicable BOOLEAN NOT NULL DEFAULT FALSE,
tds_section_id BIGINT,
tds_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
amount_paid NUMERIC(18,2) NOT NULL DEFAULT 0,
balance_due NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
approval_request_id BIGINT,
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMPTZ
```

**cfbos_vendor_invoice_line**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_invoice_id BIGINT NOT NULL REFERENCES cfbos_vendor_invoice(id),
description VARCHAR(255) NOT NULL,
hsn_sac_code VARCHAR(10),
quantity NUMERIC(18,4) NOT NULL,
rate NUMERIC(18,4) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
total_amount NUMERIC(18,2) NOT NULL,
account_id BIGINT REFERENCES cfbos_account(id)
```

**cfbos_vendor_invoice_tax_line**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_invoice_id BIGINT NOT NULL REFERENCES cfbos_vendor_invoice(id),
tax_type VARCHAR(10) NOT NULL,
tax_rate NUMERIC(5,2) NOT NULL,
taxable_amount NUMERIC(18,2) NOT NULL,
tax_amount NUMERIC(18,2) NOT NULL
```

**cfbos_vendor_payment**
```sql
id BIGSERIAL PRIMARY KEY,
payment_number VARCHAR(30) NOT NULL UNIQUE,
payment_date DATE NOT NULL,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
amount NUMERIC(18,2) NOT NULL,
tds_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
net_amount NUMERIC(18,2) NOT NULL,
payment_method VARCHAR(20) NOT NULL,
payment_reference VARCHAR(100),
bank_account_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
approval_request_id BIGINT,
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_vendor_payment_line**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_payment_id BIGINT NOT NULL REFERENCES cfbos_vendor_payment(id),
vendor_invoice_id BIGINT NOT NULL REFERENCES cfbos_vendor_invoice(id),
allocated_amount NUMERIC(18,2) NOT NULL,
tds_amount NUMERIC(18,2) NOT NULL DEFAULT 0
```

**cfbos_vendor_advance**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
amount NUMERIC(18,2) NOT NULL,
utilized_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
balance NUMERIC(18,2) NOT NULL,
payment_date DATE NOT NULL,
payment_reference VARCHAR(100),
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_vendor_debit_note**
```sql
id BIGSERIAL PRIMARY KEY,
debit_note_number VARCHAR(30) NOT NULL UNIQUE,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
vendor_invoice_id BIGINT REFERENCES cfbos_vendor_invoice(id),
debit_note_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_vendor_credit_note**
```sql
id BIGSERIAL PRIMARY KEY,
credit_note_number VARCHAR(30) NOT NULL UNIQUE,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
vendor_invoice_id BIGINT REFERENCES cfbos_vendor_invoice(id),
credit_note_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_grn**
```sql
id BIGSERIAL PRIMARY KEY,
grn_number VARCHAR(30) NOT NULL UNIQUE,
grn_date DATE NOT NULL,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
purchase_order_id BIGINT REFERENCES cfbos_purchase_order(id),
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
notes TEXT,
received_by BIGINT NOT NULL,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_grn_line**
```sql
id BIGSERIAL PRIMARY KEY,
grn_id BIGINT NOT NULL REFERENCES cfbos_grn(id),
po_line_id BIGINT REFERENCES cfbos_purchase_order_line(id),
description VARCHAR(255) NOT NULL,
ordered_quantity NUMERIC(18,4),
received_quantity NUMERIC(18,4) NOT NULL,
accepted_quantity NUMERIC(18,4) NOT NULL,
rejected_quantity NUMERIC(18,4) NOT NULL DEFAULT 0,
rejection_reason TEXT
```

**cfbos_vendor_document**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
document_type VARCHAR(50) NOT NULL,
document_name VARCHAR(255) NOT NULL,
file_path VARCHAR(500) NOT NULL,
expiry_date DATE,
is_verified BOOLEAN NOT NULL DEFAULT FALSE,
verified_by BIGINT, verified_at TIMESTAMPTZ,
uploaded_by BIGINT NOT NULL, uploaded_at TIMESTAMPTZ NOT NULL
```

**cfbos_vendor_compliance**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
compliance_type VARCHAR(50) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
due_date DATE,
completed_date DATE,
document_id BIGINT REFERENCES cfbos_vendor_document(id),
notes TEXT,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

#### 3.2.11 Fund Management (7 tables)

**cfbos_fund_type**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL UNIQUE,
code VARCHAR(20) NOT NULL UNIQUE,
description TEXT,
is_restricted BOOLEAN NOT NULL DEFAULT FALSE,
created_at TIMESTAMPTZ
```

**cfbos_fund**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
fund_type_id BIGINT NOT NULL REFERENCES cfbos_fund_type(id),
fund_code VARCHAR(20) NOT NULL UNIQUE,
purpose TEXT,
target_amount NUMERIC(18,2),
current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
is_restricted BOOLEAN NOT NULL DEFAULT FALSE,
restriction_notes TEXT,
effective_from DATE NOT NULL,
effective_to DATE,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_fund_allocation**
```sql
id BIGSERIAL PRIMARY KEY,
fund_id BIGINT NOT NULL REFERENCES cfbos_fund(id),
allocation_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
source_type VARCHAR(30) NOT NULL,
source_id BIGINT,
narration VARCHAR(255),
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_fund_transfer**
```sql
id BIGSERIAL PRIMARY KEY,
transfer_number VARCHAR(30) NOT NULL UNIQUE,
from_fund_id BIGINT NOT NULL REFERENCES cfbos_fund(id),
to_fund_id BIGINT NOT NULL REFERENCES cfbos_fund(id),
amount NUMERIC(18,2) NOT NULL,
transfer_date DATE NOT NULL,
reason TEXT NOT NULL,
approval_request_id BIGINT,
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_fund_utilization**
```sql
id BIGSERIAL PRIMARY KEY,
fund_id BIGINT NOT NULL REFERENCES cfbos_fund(id),
utilization_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
purpose TEXT NOT NULL,
expense_id BIGINT,
approval_request_id BIGINT,
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_fund_rule**
```sql
id BIGSERIAL PRIMARY KEY,
fund_id BIGINT NOT NULL REFERENCES cfbos_fund(id),
contribution_type VARCHAR(20) NOT NULL,
fixed_amount NUMERIC(18,2),
percentage NUMERIC(5,2),
source_charge_type_id BIGINT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_fund_balance**
```sql
id BIGSERIAL PRIMARY KEY,
fund_id BIGINT NOT NULL REFERENCES cfbos_fund(id),
as_of_date DATE NOT NULL,
opening_balance NUMERIC(18,2) NOT NULL,
total_inflow NUMERIC(18,2) NOT NULL DEFAULT 0,
total_outflow NUMERIC(18,2) NOT NULL DEFAULT 0,
closing_balance NUMERIC(18,2) NOT NULL,
created_at TIMESTAMPTZ
```

#### 3.2.12 Budget Management (8 tables)

**cfbos_budget**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
budget_type VARCHAR(20) NOT NULL DEFAULT 'ANNUAL',
total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
approval_request_id BIGINT,
notes TEXT,
version INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_budget_category**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
code VARCHAR(20) NOT NULL UNIQUE,
parent_id BIGINT REFERENCES cfbos_budget_category(id),
account_id BIGINT REFERENCES cfbos_account(id),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_budget_line**
```sql
id BIGSERIAL PRIMARY KEY,
budget_id BIGINT NOT NULL REFERENCES cfbos_budget(id),
budget_category_id BIGINT NOT NULL REFERENCES cfbos_budget_category(id),
account_id BIGINT REFERENCES cfbos_account(id),
allocated_amount NUMERIC(18,2) NOT NULL,
actual_spent NUMERIC(18,2) NOT NULL DEFAULT 0,
committed_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
available_amount NUMERIC(18,2) NOT NULL,
notes TEXT
```

**cfbos_budget_revision**
```sql
id BIGSERIAL PRIMARY KEY,
budget_id BIGINT NOT NULL REFERENCES cfbos_budget(id),
revision_number INTEGER NOT NULL,
revision_date DATE NOT NULL,
reason TEXT NOT NULL,
previous_total NUMERIC(18,2) NOT NULL,
revised_total NUMERIC(18,2) NOT NULL,
approval_request_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_budget_revision_line**
```sql
id BIGSERIAL PRIMARY KEY,
budget_revision_id BIGINT NOT NULL REFERENCES cfbos_budget_revision(id),
budget_line_id BIGINT NOT NULL REFERENCES cfbos_budget_line(id),
previous_amount NUMERIC(18,2) NOT NULL,
revised_amount NUMERIC(18,2) NOT NULL,
change_amount NUMERIC(18,2) NOT NULL,
reason VARCHAR(255)
```

**cfbos_budget_actual**
```sql
id BIGSERIAL PRIMARY KEY,
budget_line_id BIGINT NOT NULL REFERENCES cfbos_budget_line(id),
expense_id BIGINT NOT NULL REFERENCES cfbos_expense(id),
amount NUMERIC(18,2) NOT NULL,
recorded_date DATE NOT NULL,
created_at TIMESTAMPTZ
```

**cfbos_budget_department**
```sql
id BIGSERIAL PRIMARY KEY,
budget_id BIGINT NOT NULL REFERENCES cfbos_budget(id),
department_name VARCHAR(100) NOT NULL,
allocated_amount NUMERIC(18,2) NOT NULL,
actual_spent NUMERIC(18,2) NOT NULL DEFAULT 0,
notes TEXT,
created_at TIMESTAMPTZ
```

**cfbos_budget_project**
```sql
id BIGSERIAL PRIMARY KEY,
budget_id BIGINT NOT NULL REFERENCES cfbos_budget(id),
project_name VARCHAR(100) NOT NULL,
description TEXT,
allocated_amount NUMERIC(18,2) NOT NULL,
actual_spent NUMERIC(18,2) NOT NULL DEFAULT 0,
start_date DATE,
end_date DATE,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.13 Treasury & Banking (12 tables)

**cfbos_bank_account**
```sql
id BIGSERIAL PRIMARY KEY,
account_name VARCHAR(100) NOT NULL,
bank_name VARCHAR(100) NOT NULL,
branch_name VARCHAR(100),
account_number_encrypted VARCHAR(500) NOT NULL,
ifsc_code VARCHAR(11) NOT NULL,
account_type VARCHAR(20) NOT NULL,
current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
is_primary BOOLEAN NOT NULL DEFAULT FALSE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_bank_statement**
```sql
id BIGSERIAL PRIMARY KEY,
bank_account_id BIGINT NOT NULL REFERENCES cfbos_bank_account(id),
statement_date DATE NOT NULL,
file_name VARCHAR(255),
file_path VARCHAR(500),
format VARCHAR(20) NOT NULL,
total_entries INTEGER NOT NULL DEFAULT 0,
matched_entries INTEGER NOT NULL DEFAULT 0,
unmatched_entries INTEGER NOT NULL DEFAULT 0,
status VARCHAR(20) NOT NULL DEFAULT 'IMPORTED',
imported_by BIGINT NOT NULL, imported_at TIMESTAMPTZ NOT NULL
```

**cfbos_bank_statement_line**
```sql
id BIGSERIAL PRIMARY KEY,
bank_statement_id BIGINT NOT NULL REFERENCES cfbos_bank_statement(id),
transaction_date DATE NOT NULL,
value_date DATE,
description TEXT NOT NULL,
reference_number VARCHAR(100),
debit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
balance NUMERIC(18,2),
status VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',
matched_entity_type VARCHAR(50),
matched_entity_id BIGINT
```

**cfbos_bank_reconciliation**
```sql
id BIGSERIAL PRIMARY KEY,
bank_account_id BIGINT NOT NULL REFERENCES cfbos_bank_account(id),
reconciliation_date DATE NOT NULL,
statement_balance NUMERIC(18,2) NOT NULL,
book_balance NUMERIC(18,2) NOT NULL,
difference NUMERIC(18,2) NOT NULL DEFAULT 0,
status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
finalized_by BIGINT, finalized_at TIMESTAMPTZ,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_bank_reconciliation_line**
```sql
id BIGSERIAL PRIMARY KEY,
reconciliation_id BIGINT NOT NULL REFERENCES cfbos_bank_reconciliation(id),
statement_line_id BIGINT NOT NULL REFERENCES cfbos_bank_statement_line(id),
matched_entity_type VARCHAR(50) NOT NULL,
matched_entity_id BIGINT NOT NULL,
match_type VARCHAR(20) NOT NULL,
matched_by BIGINT, matched_at TIMESTAMPTZ
```

**cfbos_cheque**
```sql
id BIGSERIAL PRIMARY KEY,
cheque_number_encrypted VARCHAR(500) NOT NULL,
bank_account_id BIGINT NOT NULL REFERENCES cfbos_bank_account(id),
cheque_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
payee_name VARCHAR(200) NOT NULL,
cheque_type VARCHAR(20) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
clearance_date DATE,
bounce_date DATE, bounce_reason TEXT,
entity_type VARCHAR(50), entity_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_cash_book**
```sql
id BIGSERIAL PRIMARY KEY,
entry_date DATE NOT NULL,
entry_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
narration TEXT NOT NULL,
payment_id BIGINT,
receipt_id BIGINT,
journal_entry_id BIGINT,
running_balance NUMERIC(18,2) NOT NULL,
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_payment_advice**
```sql
id BIGSERIAL PRIMARY KEY,
advice_number VARCHAR(30) NOT NULL UNIQUE,
vendor_id BIGINT REFERENCES cfbos_vendor(id),
advice_date DATE NOT NULL,
total_amount NUMERIC(18,2) NOT NULL,
payment_method VARCHAR(20) NOT NULL,
bank_account_id BIGINT REFERENCES cfbos_bank_account(id),
notes TEXT,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_fixed_deposit**
```sql
id BIGSERIAL PRIMARY KEY,
fd_number VARCHAR(50) NOT NULL,
bank_account_id BIGINT NOT NULL REFERENCES cfbos_bank_account(id),
principal_amount NUMERIC(18,2) NOT NULL,
interest_rate NUMERIC(5,2) NOT NULL,
start_date DATE NOT NULL,
maturity_date DATE NOT NULL,
maturity_amount NUMERIC(18,2) NOT NULL,
interest_frequency VARCHAR(20) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
account_id BIGINT REFERENCES cfbos_account(id),
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_bank_charge**
```sql
id BIGSERIAL PRIMARY KEY,
bank_account_id BIGINT NOT NULL REFERENCES cfbos_bank_account(id),
charge_date DATE NOT NULL,
charge_type VARCHAR(50) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
description VARCHAR(255),
journal_entry_id BIGINT,
created_at TIMESTAMPTZ
```

**cfbos_petty_cash**
```sql
id BIGSERIAL PRIMARY KEY,
custodian_id BIGINT NOT NULL,
limit_amount NUMERIC(18,2) NOT NULL,
current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_petty_cash_txn**
```sql
id BIGSERIAL PRIMARY KEY,
petty_cash_id BIGINT NOT NULL REFERENCES cfbos_petty_cash(id),
transaction_date DATE NOT NULL,
transaction_type VARCHAR(20) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
description TEXT NOT NULL,
expense_category_id BIGINT,
receipt_path VARCHAR(500),
journal_entry_id BIGINT,
created_by BIGINT, created_at TIMESTAMPTZ
```

#### 3.2.14 Tax Engine (12 tables)

**cfbos_tax_config**
```sql
id BIGSERIAL PRIMARY KEY,
community_gstin VARCHAR(15),
community_state_code VARCHAR(2),
is_gst_registered BOOLEAN NOT NULL DEFAULT FALSE,
default_gst_rate NUMERIC(5,2) NOT NULL DEFAULT 18.00,
default_cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 9.00,
default_sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 9.00,
financial_year_start_month INTEGER NOT NULL DEFAULT 4,
updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_tax_rate**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(50) NOT NULL,
tax_type VARCHAR(10) NOT NULL,
rate NUMERIC(5,2) NOT NULL,
cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
igst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
effective_from DATE NOT NULL,
effective_to DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_hsn_sac_code**
```sql
id BIGSERIAL PRIMARY KEY,
code VARCHAR(10) NOT NULL UNIQUE,
description VARCHAR(255) NOT NULL,
code_type VARCHAR(3) NOT NULL,
default_gst_rate NUMERIC(5,2),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_tds_section**
```sql
id BIGSERIAL PRIMARY KEY,
section_code VARCHAR(10) NOT NULL UNIQUE,
description VARCHAR(255) NOT NULL,
individual_rate NUMERIC(5,2) NOT NULL,
company_rate NUMERIC(5,2) NOT NULL,
threshold_amount NUMERIC(18,2),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_tds_rate**
```sql
id BIGSERIAL PRIMARY KEY,
tds_section_id BIGINT NOT NULL REFERENCES cfbos_tds_section(id),
payee_type VARCHAR(20) NOT NULL,
rate NUMERIC(5,2) NOT NULL,
surcharge NUMERIC(5,2) NOT NULL DEFAULT 0,
cess NUMERIC(5,2) NOT NULL DEFAULT 0,
effective_from DATE NOT NULL,
effective_to DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE
```

**cfbos_tds_deduction**
```sql
id BIGSERIAL PRIMARY KEY,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
tds_section_id BIGINT NOT NULL REFERENCES cfbos_tds_section(id),
vendor_invoice_id BIGINT REFERENCES cfbos_vendor_invoice(id),
vendor_payment_id BIGINT REFERENCES cfbos_vendor_payment(id),
deduction_date DATE NOT NULL,
gross_amount NUMERIC(18,2) NOT NULL,
tds_rate NUMERIC(5,2) NOT NULL,
tds_amount NUMERIC(18,2) NOT NULL,
surcharge NUMERIC(18,2) NOT NULL DEFAULT 0,
cess NUMERIC(18,2) NOT NULL DEFAULT 0,
total_tds NUMERIC(18,2) NOT NULL,
challan_id BIGINT,
certificate_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'DEDUCTED',
journal_entry_id BIGINT,
created_at TIMESTAMPTZ
```

**cfbos_tds_certificate**
```sql
id BIGSERIAL PRIMARY KEY,
certificate_number VARCHAR(30) NOT NULL UNIQUE,
vendor_id BIGINT NOT NULL REFERENCES cfbos_vendor(id),
financial_year VARCHAR(9) NOT NULL,
quarter VARCHAR(2) NOT NULL,
total_amount_paid NUMERIC(18,2) NOT NULL,
total_tds_deducted NUMERIC(18,2) NOT NULL,
generated_date DATE NOT NULL,
file_path VARCHAR(500),
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_gst_return**
```sql
id BIGSERIAL PRIMARY KEY,
return_type VARCHAR(10) NOT NULL,
period VARCHAR(10) NOT NULL,
financial_year VARCHAR(9) NOT NULL,
filing_due_date DATE NOT NULL,
filed_date DATE,
total_taxable NUMERIC(18,2) NOT NULL DEFAULT 0,
total_cgst NUMERIC(18,2) NOT NULL DEFAULT 0,
total_sgst NUMERIC(18,2) NOT NULL DEFAULT 0,
total_igst NUMERIC(18,2) NOT NULL DEFAULT 0,
total_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
file_path VARCHAR(500),
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_gst_return_line**
```sql
id BIGSERIAL PRIMARY KEY,
gst_return_id BIGINT NOT NULL REFERENCES cfbos_gst_return(id),
hsn_sac_code VARCHAR(10),
taxable_amount NUMERIC(18,2) NOT NULL,
cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
source_type VARCHAR(20) NOT NULL,
source_id BIGINT
```

**cfbos_tax_exemption**
```sql
id BIGSERIAL PRIMARY KEY,
entity_type VARCHAR(50) NOT NULL,
entity_id BIGINT NOT NULL,
tax_type VARCHAR(10) NOT NULL,
exemption_reason TEXT NOT NULL,
certificate_number VARCHAR(50),
effective_from DATE NOT NULL,
effective_to DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_gst_input_credit**
```sql
id BIGSERIAL PRIMARY KEY,
period VARCHAR(10) NOT NULL,
vendor_invoice_id BIGINT REFERENCES cfbos_vendor_invoice(id),
expense_id BIGINT REFERENCES cfbos_expense(id),
cgst_credit NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_credit NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_credit NUMERIC(18,2) NOT NULL DEFAULT 0,
total_credit NUMERIC(18,2) NOT NULL,
is_eligible BOOLEAN NOT NULL DEFAULT TRUE,
reversal_reason TEXT,
created_at TIMESTAMPTZ
```

**cfbos_gst_output_liability**
```sql
id BIGSERIAL PRIMARY KEY,
period VARCHAR(10) NOT NULL,
invoice_id BIGINT REFERENCES cfbos_invoice(id),
cgst_liability NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst_liability NUMERIC(18,2) NOT NULL DEFAULT 0,
igst_liability NUMERIC(18,2) NOT NULL DEFAULT 0,
total_liability NUMERIC(18,2) NOT NULL,
created_at TIMESTAMPTZ
```

#### 3.2.15 Reporting (6 tables)

**cfbos_report_config**
```sql
id BIGSERIAL PRIMARY KEY,
report_type VARCHAR(50) NOT NULL UNIQUE,
display_name VARCHAR(100) NOT NULL,
description TEXT,
default_parameters JSONB,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

**cfbos_report_schedule**
```sql
id BIGSERIAL PRIMARY KEY,
report_config_id BIGINT NOT NULL REFERENCES cfbos_report_config(id),
frequency VARCHAR(20) NOT NULL,
parameters JSONB,
recipients TEXT[],
next_run_date DATE NOT NULL,
last_run_date DATE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_report_archive**
```sql
id BIGSERIAL PRIMARY KEY,
report_config_id BIGINT NOT NULL REFERENCES cfbos_report_config(id),
report_name VARCHAR(100) NOT NULL,
parameters JSONB,
file_path VARCHAR(500) NOT NULL,
file_format VARCHAR(10) NOT NULL,
generated_by BIGINT NOT NULL,
generated_at TIMESTAMPTZ NOT NULL
```

**cfbos_financial_snapshot**
```sql
id BIGSERIAL PRIMARY KEY,
snapshot_date DATE NOT NULL,
snapshot_type VARCHAR(30) NOT NULL,
data JSONB NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_financial_statement**
```sql
id BIGSERIAL PRIMARY KEY,
statement_type VARCHAR(30) NOT NULL,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
period_start DATE NOT NULL,
period_end DATE NOT NULL,
data JSONB NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
approved_by BIGINT, approved_at TIMESTAMPTZ,
file_path VARCHAR(500),
created_by BIGINT, created_at TIMESTAMPTZ
```

**cfbos_statement_line**
```sql
id BIGSERIAL PRIMARY KEY,
financial_statement_id BIGINT NOT NULL REFERENCES cfbos_financial_statement(id),
line_order INTEGER NOT NULL,
account_id BIGINT REFERENCES cfbos_account(id),
label VARCHAR(200) NOT NULL,
amount NUMERIC(18,2) NOT NULL,
indent_level INTEGER NOT NULL DEFAULT 0,
is_total_line BOOLEAN NOT NULL DEFAULT FALSE,
is_bold BOOLEAN NOT NULL DEFAULT FALSE
```

#### 3.2.16 Resident Portal (4 tables)

**cfbos_resident_dispute**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
invoice_id BIGINT NOT NULL REFERENCES cfbos_invoice(id),
dispute_type VARCHAR(30) NOT NULL,
description TEXT NOT NULL,
supporting_document_path VARCHAR(500),
status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
resolution TEXT,
resolved_by BIGINT, resolved_at TIMESTAMPTZ,
helpdesk_ticket_id BIGINT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ
```

**cfbos_resident_query**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
query_type VARCHAR(30) NOT NULL,
subject VARCHAR(200) NOT NULL,
description TEXT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
response TEXT,
responded_by BIGINT, responded_at TIMESTAMPTZ,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_resident_auto_debit**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL UNIQUE,
is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
mandate_id BIGINT REFERENCES cfbos_auto_debit_mandate(id),
max_amount NUMERIC(18,2),
updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_resident_billing_profile**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
property_id BIGINT NOT NULL,
resident_category VARCHAR(30) NOT NULL DEFAULT 'OWNER',
is_billing_active BOOLEAN NOT NULL DEFAULT TRUE,
preferred_payment_method VARCHAR(20),
preferred_notification_channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
communication_email VARCHAR(255),
communication_phone VARCHAR(20),
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ,
UNIQUE(resident_id, property_id)
```

#### 3.2.17 AI Finance (4 tables)

**cfbos_ai_query_log**
```sql
id BIGSERIAL PRIMARY KEY,
user_id BIGINT NOT NULL,
query_text TEXT NOT NULL,
response_text TEXT,
query_type VARCHAR(30),
execution_time_ms INTEGER,
was_successful BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_ai_insight**
```sql
id BIGSERIAL PRIMARY KEY,
insight_type VARCHAR(50) NOT NULL,
title VARCHAR(200) NOT NULL,
description TEXT NOT NULL,
severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
data JSONB,
is_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
acknowledged_by BIGINT, acknowledged_at TIMESTAMPTZ,
generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_ai_prediction**
```sql
id BIGSERIAL PRIMARY KEY,
prediction_type VARCHAR(50) NOT NULL,
prediction_date DATE NOT NULL,
target_period_start DATE NOT NULL,
target_period_end DATE NOT NULL,
prediction_data JSONB NOT NULL,
confidence_score NUMERIC(5,2),
actual_data JSONB,
accuracy_score NUMERIC(5,2),
model_version VARCHAR(20),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_financial_health_score**
```sql
id BIGSERIAL PRIMARY KEY,
score_date DATE NOT NULL,
overall_score NUMERIC(5,2) NOT NULL,
collection_score NUMERIC(5,2) NOT NULL,
expense_score NUMERIC(5,2) NOT NULL,
fund_score NUMERIC(5,2) NOT NULL,
compliance_score NUMERIC(5,2) NOT NULL,
liquidity_score NUMERIC(5,2) NOT NULL,
score_details JSONB NOT NULL,
recommendations JSONB,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

#### 3.2.18 Analytics (4 tables)

**cfbos_collection_metric**
```sql
id BIGSERIAL PRIMARY KEY,
period_date DATE NOT NULL,
period_type VARCHAR(10) NOT NULL,
total_billed NUMERIC(18,2) NOT NULL DEFAULT 0,
total_collected NUMERIC(18,2) NOT NULL DEFAULT 0,
total_outstanding NUMERIC(18,2) NOT NULL DEFAULT 0,
collection_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
on_time_payments INTEGER NOT NULL DEFAULT 0,
late_payments INTEGER NOT NULL DEFAULT 0,
defaulters_count INTEGER NOT NULL DEFAULT 0,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_expense_metric**
```sql
id BIGSERIAL PRIMARY KEY,
period_date DATE NOT NULL,
period_type VARCHAR(10) NOT NULL,
total_expenses NUMERIC(18,2) NOT NULL DEFAULT 0,
budgeted_expenses NUMERIC(18,2) NOT NULL DEFAULT 0,
variance NUMERIC(18,2) NOT NULL DEFAULT 0,
variance_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
top_categories JSONB,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_cash_flow_metric**
```sql
id BIGSERIAL PRIMARY KEY,
period_date DATE NOT NULL,
period_type VARCHAR(10) NOT NULL,
total_inflow NUMERIC(18,2) NOT NULL DEFAULT 0,
total_outflow NUMERIC(18,2) NOT NULL DEFAULT 0,
net_cash_flow NUMERIC(18,2) NOT NULL DEFAULT 0,
opening_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
closing_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
inflow_breakdown JSONB,
outflow_breakdown JSONB,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_kpi_snapshot**
```sql
id BIGSERIAL PRIMARY KEY,
snapshot_date DATE NOT NULL,
kpi_name VARCHAR(50) NOT NULL,
kpi_value NUMERIC(18,4) NOT NULL,
kpi_unit VARCHAR(20),
trend VARCHAR(10),
previous_value NUMERIC(18,4),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

#### 3.2.19 Security & Audit (4 tables)

**cfbos_financial_audit_trail**
```sql
id BIGSERIAL PRIMARY KEY,
event_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
event_type VARCHAR(50) NOT NULL,
entity_type VARCHAR(100) NOT NULL,
entity_id BIGINT NOT NULL,
action VARCHAR(20) NOT NULL,
actor_id BIGINT NOT NULL,
actor_role VARCHAR(50),
actor_ip VARCHAR(45),
before_state JSONB,
after_state JSONB,
event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
checksum VARCHAR(64) NOT NULL
```

**cfbos_maker_checker_config**
```sql
id BIGSERIAL PRIMARY KEY,
entity_type VARCHAR(100) NOT NULL,
operation VARCHAR(30) NOT NULL,
is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
min_amount_threshold NUMERIC(18,2),
maker_role VARCHAR(50) NOT NULL,
checker_role VARCHAR(50) NOT NULL,
updated_by BIGINT, updated_at TIMESTAMPTZ,
UNIQUE(entity_type, operation)
```

**cfbos_digital_signature**
```sql
id BIGSERIAL PRIMARY KEY,
entity_type VARCHAR(100) NOT NULL,
entity_id BIGINT NOT NULL,
signer_id BIGINT NOT NULL,
signature_hash VARCHAR(256) NOT NULL,
algorithm VARCHAR(20) NOT NULL DEFAULT 'SHA-256',
signed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
is_valid BOOLEAN NOT NULL DEFAULT TRUE
```

**cfbos_financial_log**
```sql
id BIGSERIAL PRIMARY KEY,
operation_type VARCHAR(50) NOT NULL,
module VARCHAR(30) NOT NULL,
entity_type VARCHAR(100),
entity_id BIGINT,
description TEXT NOT NULL,
amount NUMERIC(18,2),
user_id BIGINT NOT NULL,
user_ip VARCHAR(45),
session_id VARCHAR(100),
request_id VARCHAR(100),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

#### 3.2.20 Integration (3 tables)

**cfbos_integration_config**
```sql
id BIGSERIAL PRIMARY KEY,
integration_name VARCHAR(50) NOT NULL UNIQUE,
integration_type VARCHAR(30) NOT NULL,
config_json JSONB NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
last_sync_at TIMESTAMPTZ,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_integration_log**
```sql
id BIGSERIAL PRIMARY KEY,
integration_name VARCHAR(50) NOT NULL,
event_type VARCHAR(50) NOT NULL,
direction VARCHAR(10) NOT NULL,
payload JSONB,
response JSONB,
status VARCHAR(20) NOT NULL,
error_message TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_webhook**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
url VARCHAR(500) NOT NULL,
events TEXT[] NOT NULL,
secret_encrypted VARCHAR(500),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
last_triggered_at TIMESTAMPTZ,
failure_count INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.21 Automation (5 tables)

**cfbos_automation_rule**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
description TEXT,
trigger_event VARCHAR(50) NOT NULL,
condition_expression TEXT,
action_type VARCHAR(50) NOT NULL,
action_config JSONB NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
priority INTEGER NOT NULL DEFAULT 0,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_automation_run**
```sql
id BIGSERIAL PRIMARY KEY,
automation_rule_id BIGINT NOT NULL REFERENCES cfbos_automation_rule(id),
trigger_event VARCHAR(50) NOT NULL,
trigger_entity_type VARCHAR(100),
trigger_entity_id BIGINT,
status VARCHAR(20) NOT NULL,
result JSONB,
error_message TEXT,
started_at TIMESTAMPTZ NOT NULL,
completed_at TIMESTAMPTZ
```

**cfbos_automation_schedule**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
job_type VARCHAR(50) NOT NULL,
cron_expression VARCHAR(50) NOT NULL,
parameters JSONB,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
last_run_at TIMESTAMPTZ,
next_run_at TIMESTAMPTZ,
last_run_status VARCHAR(20),
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_reminder_schedule**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
reminder_type VARCHAR(30) NOT NULL,
days_offset INTEGER NOT NULL,
offset_from VARCHAR(20) NOT NULL,
template_subject VARCHAR(255),
template_body TEXT,
channels TEXT[] NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_reminder_log**
```sql
id BIGSERIAL PRIMARY KEY,
reminder_schedule_id BIGINT NOT NULL REFERENCES cfbos_reminder_schedule(id),
resident_id BIGINT NOT NULL,
invoice_id BIGINT,
channel VARCHAR(20) NOT NULL,
status VARCHAR(20) NOT NULL,
sent_at TIMESTAMPTZ,
error_message TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

#### 3.2.22 Revenue Model (5 tables)

**cfbos_subscription_plan**
```sql
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
code VARCHAR(20) NOT NULL UNIQUE,
description TEXT,
tier VARCHAR(20) NOT NULL,
monthly_price NUMERIC(18,2) NOT NULL,
annual_price NUMERIC(18,2),
max_properties INTEGER,
features JSONB NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_subscription**
```sql
id BIGSERIAL PRIMARY KEY,
plan_id BIGINT NOT NULL REFERENCES cfbos_subscription_plan(id),
start_date DATE NOT NULL,
end_date DATE,
billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_subscription_invoice**
```sql
id BIGSERIAL PRIMARY KEY,
subscription_id BIGINT NOT NULL REFERENCES cfbos_subscription(id),
invoice_date DATE NOT NULL,
due_date DATE NOT NULL,
amount NUMERIC(18,2) NOT NULL,
cgst NUMERIC(18,2) NOT NULL DEFAULT 0,
sgst NUMERIC(18,2) NOT NULL DEFAULT 0,
total NUMERIC(18,2) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
payment_reference VARCHAR(100),
paid_date DATE,
created_at TIMESTAMPTZ
```

**cfbos_api_usage**
```sql
id BIGSERIAL PRIMARY KEY,
usage_date DATE NOT NULL,
endpoint VARCHAR(200) NOT NULL,
request_count INTEGER NOT NULL DEFAULT 0,
created_at TIMESTAMPTZ
```

**cfbos_api_rate_plan**
```sql
id BIGSERIAL PRIMARY KEY,
plan_id BIGINT NOT NULL REFERENCES cfbos_subscription_plan(id),
daily_limit INTEGER NOT NULL,
monthly_limit INTEGER NOT NULL,
rate_per_extra_request NUMERIC(10,4),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ
```

#### 3.2.23 Collections (5 tables)

**cfbos_defaulter_list**
```sql
id BIGSERIAL PRIMARY KEY,
resident_id BIGINT NOT NULL,
property_id BIGINT NOT NULL,
total_outstanding NUMERIC(18,2) NOT NULL,
oldest_invoice_date DATE NOT NULL,
days_overdue INTEGER NOT NULL,
category VARCHAR(20) NOT NULL,
last_payment_date DATE,
last_reminder_date DATE,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

**cfbos_legal_notice**
```sql
id BIGSERIAL PRIMARY KEY,
notice_number VARCHAR(30) NOT NULL UNIQUE,
resident_id BIGINT NOT NULL,
property_id BIGINT NOT NULL,
notice_date DATE NOT NULL,
notice_type VARCHAR(30) NOT NULL,
total_outstanding NUMERIC(18,2) NOT NULL,
content TEXT NOT NULL,
sent_via VARCHAR(20),
sent_date DATE,
response_received BOOLEAN NOT NULL DEFAULT FALSE,
response_text TEXT,
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_collection_agent**
```sql
id BIGSERIAL PRIMARY KEY,
agent_name VARCHAR(100) NOT NULL,
agent_type VARCHAR(20) NOT NULL,
assigned_properties BIGINT[],
commission_rate NUMERIC(5,2),
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**cfbos_write_off**
```sql
id BIGSERIAL PRIMARY KEY,
write_off_number VARCHAR(30) NOT NULL UNIQUE,
resident_id BIGINT NOT NULL,
write_off_date DATE NOT NULL,
total_amount NUMERIC(18,2) NOT NULL,
reason TEXT NOT NULL,
approval_request_id BIGINT NOT NULL,
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_settlement**
```sql
id BIGSERIAL PRIMARY KEY,
settlement_number VARCHAR(30) NOT NULL UNIQUE,
resident_id BIGINT NOT NULL,
settlement_date DATE NOT NULL,
original_amount NUMERIC(18,2) NOT NULL,
settlement_amount NUMERIC(18,2) NOT NULL,
discount_given NUMERIC(18,2) NOT NULL DEFAULT 0,
reason TEXT NOT NULL,
approval_request_id BIGINT,
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.24 Assets (3 tables)

**cfbos_asset_register**
```sql
id BIGSERIAL PRIMARY KEY,
asset_code VARCHAR(20) NOT NULL UNIQUE,
name VARCHAR(200) NOT NULL,
description TEXT,
category VARCHAR(50) NOT NULL,
purchase_date DATE NOT NULL,
purchase_cost NUMERIC(18,2) NOT NULL,
current_value NUMERIC(18,2) NOT NULL,
salvage_value NUMERIC(18,2) NOT NULL DEFAULT 0,
useful_life_years INTEGER NOT NULL,
depreciation_method VARCHAR(20) NOT NULL DEFAULT 'SLM',
location VARCHAR(200),
account_id BIGINT REFERENCES cfbos_account(id),
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

**cfbos_depreciation_schedule**
```sql
id BIGSERIAL PRIMARY KEY,
asset_id BIGINT NOT NULL REFERENCES cfbos_asset_register(id),
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
period_start DATE NOT NULL,
period_end DATE NOT NULL,
opening_value NUMERIC(18,2) NOT NULL,
depreciation_amount NUMERIC(18,2) NOT NULL,
closing_value NUMERIC(18,2) NOT NULL,
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'CALCULATED',
created_at TIMESTAMPTZ
```

**cfbos_provision**
```sql
id BIGSERIAL PRIMARY KEY,
provision_type VARCHAR(30) NOT NULL,
description TEXT NOT NULL,
amount NUMERIC(18,2) NOT NULL,
provision_date DATE NOT NULL,
fiscal_year_id BIGINT NOT NULL REFERENCES cfbos_fiscal_year(id),
account_id BIGINT NOT NULL REFERENCES cfbos_account(id),
journal_entry_id BIGINT,
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_by BIGINT, created_at TIMESTAMPTZ, updated_by BIGINT, updated_at TIMESTAMPTZ
```

#### 3.2.25 History Tables (6 tables)

All history tables follow the same pattern:
```sql
id BIGSERIAL PRIMARY KEY,
{entity}_id BIGINT NOT NULL,
revision INTEGER NOT NULL,
change_type VARCHAR(20) NOT NULL,
snapshot JSONB NOT NULL,
changed_by BIGINT NOT NULL,
changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

Tables: `cfbos_invoice_history` (already listed above), `cfbos_payment_history`, `cfbos_journal_entry_history`, `cfbos_vendor_invoice_history`, `cfbos_budget_history`, `cfbos_billing_rule_history`

### 3.3 Indexing Strategy

```sql
-- Invoice lookups
CREATE INDEX idx_cfbos_invoice_resident_status ON cfbos_invoice(resident_id, status);
CREATE INDEX idx_cfbos_invoice_property_period ON cfbos_invoice(property_id, billing_period_start);
CREATE INDEX idx_cfbos_invoice_due_date ON cfbos_invoice(due_date) WHERE status IN ('SENT', 'OVERDUE');
CREATE INDEX idx_cfbos_invoice_billing_run ON cfbos_invoice(billing_run_id);

-- Payment lookups
CREATE INDEX idx_cfbos_payment_resident_date ON cfbos_payment(resident_id, payment_date);
CREATE INDEX idx_cfbos_payment_status ON cfbos_payment(status) WHERE status = 'INITIATED';
CREATE INDEX idx_cfbos_payment_gateway ON cfbos_payment(gateway_reference);

-- Accounting lookups
CREATE INDEX idx_cfbos_journal_line_account ON cfbos_journal_line(account_id);
CREATE INDEX idx_cfbos_journal_entry_date ON cfbos_journal_entry(entry_date, status);
CREATE INDEX idx_cfbos_journal_entry_source ON cfbos_journal_entry(source_module, source_document_id);
CREATE INDEX idx_cfbos_ledger_account_date ON cfbos_ledger(account_id, posting_date);
CREATE INDEX idx_cfbos_sub_ledger_party ON cfbos_sub_ledger(ledger_type, party_id, posting_date);

-- Billing lookups
CREATE INDEX idx_cfbos_billing_run_period ON cfbos_billing_run(billing_period_start, status);
CREATE INDEX idx_cfbos_billing_rule_active ON cfbos_billing_rule(billing_category_id) WHERE is_active = TRUE AND is_deleted = FALSE;

-- Vendor lookups
CREATE INDEX idx_cfbos_vendor_invoice_vendor ON cfbos_vendor_invoice(vendor_id, status);
CREATE INDEX idx_cfbos_vendor_payment_vendor ON cfbos_vendor_payment(vendor_id, payment_date);

-- Audit trail (append-only, query-heavy)
CREATE INDEX idx_cfbos_audit_entity ON cfbos_financial_audit_trail(entity_type, entity_id);
CREATE INDEX idx_cfbos_audit_time ON cfbos_financial_audit_trail(event_time);
CREATE INDEX idx_cfbos_audit_actor ON cfbos_financial_audit_trail(actor_id);
```

### 3.4 Partitioning Strategy

```sql
-- Partition large transactional tables by fiscal year
CREATE TABLE cfbos_journal_entry (...) PARTITION BY RANGE (entry_date);
CREATE TABLE cfbos_journal_entry_fy2025 PARTITION OF cfbos_journal_entry
    FOR VALUES FROM ('2025-04-01') TO ('2026-04-01');
CREATE TABLE cfbos_journal_entry_fy2026 PARTITION OF cfbos_journal_entry
    FOR VALUES FROM ('2026-04-01') TO ('2027-04-01');

-- Same for: cfbos_ledger, cfbos_payment, cfbos_financial_audit_trail
```

### 3.5 Default Chart of Accounts (Seeded Data)

```
1000  ASSETS
  1100  Current Assets
    1110  Bank Accounts
      1111  Primary Current Account
      1112  Savings Account
    1120  Resident Receivables
    1130  GST Input Credit
    1140  Advances & Deposits
    1150  Petty Cash
  1200  Fixed Assets
    1210  Land & Building
    1220  Equipment
    1230  Accumulated Depreciation

2000  LIABILITIES
  2100  Current Liabilities
    2110  Vendor Payables
    2120  GST Payable (CGST)
    2121  GST Payable (SGST)
    2130  TDS Payable
    2140  Security Deposits (Residents)
    2150  Advance from Residents (Wallet)
  2200  Provisions
    2210  Provision for Bad Debts

3000  EQUITY / FUNDS
  3100  Corpus Fund
  3200  Sinking Fund
  3300  Maintenance Fund
  3400  Emergency Fund
  3500  Surplus / Deficit

4000  INCOME
  4100  Maintenance Income
  4200  Water Charges Income
  4300  Parking Income
  4400  Club House Income
  4500  Interest Income (FD/Bank)
  4600  Penalty & Late Fee Income
  4700  Other Income

5000  EXPENSES
  5100  Security Expenses
  5200  Housekeeping
  5300  Gardening & Landscaping
  5400  Repairs & Maintenance
  5500  Electricity (Common Area)
  5600  Water (Common Area)
  5700  Insurance
  5800  Management & Admin
  5900  Depreciation
```

---

## 4. Business Engines

### 4.1 Engine Design Pattern

Every CFBOS engine follows:

```
Engine Interface (contract)
  └── Engine Implementation (stateless Spring @Service)
        ├── reads Configuration from DB (rules, rates, formulas)
        ├── executes Business Logic (calculation, validation)
        ├── produces Result Object (immutable)
        ├── publishes Domain Event (Spring ApplicationEvent)
        └── delegates Accounting to AccountingEngine (auto journal entry)
```

### 4.2 Billing Engine

Orchestrates billing cycle: reads rules → calculates charges per property → produces billing run results.

```java
public interface BillingEngine {
    BillingRunResult executeBillingRun(BillingRunRequest request);
    BillingRunResult previewBillingRun(BillingRunRequest request);
    List<ChargeLineResult> calculateCharges(Long propertyId, BillingPeriod period);
    BillingRunResult executeSupplementaryRun(Long originalRunId, List<Long> ruleIds);
}
```

Flow: BillingRunRequest → Load active rules → For each property: filter applicable rules → ChargeCalculationEngine → TaxEngine → PenaltyEngine → aggregate → persist → publish BillingRunCompletedEvent → InvoiceEngine generates invoices.

### 4.3 Charge Calculation Engine

Pure calculation, no side effects.

```java
public interface ChargeCalculationEngine {
    ChargeResult calculate(BillingRule rule, PropertyContext context);
    ChargeResult calculateWithSlab(SlabConfig slab, BigDecimal quantity);
    ChargeResult calculateWithFormula(Formula formula, Map<String, BigDecimal> variables);
}
```

Methods: FIXED, AREA_BASED, UNIT_BASED, CONSUMPTION_BASED, SLAB_BASED, FORMULA_BASED (SpEL), OCCUPANCY_BASED.

### 4.4 Invoice Engine

Transforms billing run charges into formatted, numbered, tax-compliant invoices.

```java
public interface InvoiceEngine {
    List<Invoice> generateFromBillingRun(Long billingRunId);
    Invoice createManualInvoice(ManualInvoiceRequest request);
    List<Invoice> processRecurringInvoices(LocalDate runDate);
    CreditNote issueCreditNote(CreditNoteRequest request);
    DebitNote issueDebitNote(DebitNoteRequest request);
    Invoice cancelInvoice(Long invoiceId, String reason);
}
```

Document numbering: `{PREFIX}-{FY}-{SEQUENCE}` (e.g., INV-2026-000142). Thread-safe via `SELECT ... FOR UPDATE` on sequence row.

### 4.5 Payment Engine

```java
public interface PaymentEngine {
    PaymentResult processPayment(PaymentRequest request);
    AllocationResult autoAllocate(Long paymentId);
    AllocationResult manualAllocate(Long paymentId, List<AllocationLine> lines);
    RefundResult processRefund(RefundRequest request);
    BulkPaymentResult processBulkPayment(List<PaymentRequest> requests);
    PaymentResult handleGatewayCallback(GatewayCallbackRequest callback);
}
```

Allocation strategy: FIFO by default (oldest invoice first). Overpayment → credit to wallet.

### 4.6 Penalty & Interest Engine

```java
public interface PenaltyEngine {
    List<PenaltyResult> calculatePenalties(LocalDate asOfDate);
    PenaltyResult calculateForInvoice(Long invoiceId, LocalDate asOfDate);
    PenaltyWaiverResult requestWaiver(PenaltyWaiverRequest request);
    InterestResult calculateInterest(Long invoiceId, LocalDate fromDate, LocalDate toDate);
}
```

Configurable: grace period, fixed/percentage late fee, simple/compound interest, max cap, auto-apply.

### 4.7 Accounting Engine

```java
public interface AccountingEngine {
    JournalEntry createJournalEntry(JournalEntryRequest request);
    JournalEntry postJournalEntry(Long journalEntryId);
    JournalEntry reverseJournalEntry(Long journalEntryId, String reason);
    LedgerStatement getAccountLedger(Long accountId, DateRange period);
    SubLedgerStatement getResidentLedger(Long residentId, DateRange period);
    SubLedgerStatement getVendorLedger(Long vendorId, DateRange period);
    TrialBalance generateTrialBalance(Long fiscalYearId, LocalDate asOfDate);
    IncomeStatement generateIncomeStatement(DateRange period);
    BalanceSheet generateBalanceSheet(LocalDate asOfDate);
    CashFlowStatement generateCashFlow(DateRange period);
    void closeAccountingPeriod(Long periodId);
    void executeFiscalYearClosing(Long fiscalYearId);
}
```

Every journal entry enforces: total debits = total credits. Each line is strictly debit OR credit, never both.

### 4.8 Tax Engine

```java
public interface TaxEngine {
    TaxResult calculateGST(TaxableAmount amount, String hsnSacCode, boolean isInterState);
    TdsResult calculateTDS(BigDecimal amount, String tdsSection, Long vendorId);
    GSTReturnData prepareGSTR1(DateRange period);
    GSTReturnData prepareGSTR3B(DateRange period);
    TDSReturnData prepareTDSReturn(String quarter, String financialYear);
    TdsCertificate generateTdsCertificate(Long vendorId, String financialYear);
}
```

GST: Intra-state CGST 9% + SGST 9%, inter-state IGST 18%. SAC 9995 for maintenance.
TDS: 194C (contractors), 194J (professionals), 194I (rent), 194H (commission), 194A (interest).

### 4.9 Wallet Engine

```java
public interface WalletEngine {
    Wallet getOrCreateWallet(Long residentId);
    WalletTransaction credit(WalletCreditRequest request);
    WalletTransaction debit(WalletDebitRequest request);
    WalletTransaction adjust(WalletAdjustmentRequest request);
    BigDecimal getBalance(Long residentId);
    SecurityDeposit recordSecurityDeposit(SecurityDepositRequest request);
    RefundResult refundSecurityDeposit(Long depositId);
}
```

### 4.10 Fund Management Engine

```java
public interface FundEngine {
    Fund createFund(FundCreateRequest request);
    FundTransferResult transferBetweenFunds(FundTransferRequest request);
    FundUtilization recordUtilization(FundUtilizationRequest request);
    FundStatement getFundStatement(Long fundId, DateRange period);
    BigDecimal getFundBalance(Long fundId);
}
```

Each fund linked to a dedicated chart of accounts entry. Fund transactions auto-produce journal entries.

### 4.11 Budget Engine

```java
public interface BudgetEngine {
    Budget createBudget(BudgetCreateRequest request);
    BudgetRevision reviseBudget(BudgetRevisionRequest request);
    VarianceReport getVarianceAnalysis(Long budgetId, LocalDate asOfDate);
    boolean checkBudgetAvailability(Long budgetLineId, BigDecimal amount);
    void alertOnThresholdBreach(Long budgetId, int thresholdPercent);
}
```

Budget enforcement configurable (warning vs hard-stop when budget exhausted).

### 4.12 Bank Reconciliation Engine

```java
public interface ReconciliationEngine {
    BankStatement importStatement(Long bankAccountId, MultipartFile file, String format);
    ReconciliationResult autoReconcile(Long statementId);
    void manualMatch(Long statementLineId, String sourceType, Long sourceId);
    ReconciliationSummary finalize(Long reconciliationId);
}
```

Auto-matching rules: exact amount + reference → auto-match; exact amount + date within 3 days → suggest; UTR match → auto-match.

### 4.13 Approval Workflow Engine

```java
public interface ApprovalEngine {
    ApprovalRequest submit(ApprovalSubmitRequest request);
    ApprovalRequest approve(Long requestId, Long approverId, String comments);
    ApprovalRequest reject(Long requestId, Long approverId, String reason);
    List<ApprovalRequest> getPendingApprovals(Long approverId);
}
```

Configurable per entity type with amount-based step routing and multi-level approvals.

### 4.14 Reporting Engine

```java
public interface ReportingEngine {
    Report generateIncomeExpenseStatement(DateRange period);
    Report generateBalanceSheet(LocalDate asOfDate);
    Report generateCashFlowStatement(DateRange period);
    Report generateFundStatement(Long fundId, DateRange period);
    Report generateCollectionReport(DateRange period);
    Report generateOutstandingReport(LocalDate asOfDate);
    Report generateDefaulterReport(int daysOverdue);
    Report generateAgeingReport(LocalDate asOfDate);
    Report generateVendorReport(DateRange period);
    Report generateResidentStatement(Long residentId, DateRange period);
    Report generateBudgetVarianceReport(Long budgetId);
    Report generateTaxReport(DateRange period);
    byte[] exportToPdf(Report report);
    byte[] exportToExcel(Report report);
}
```

### 4.15 AI Finance Engine

```java
public interface AIFinanceEngine {
    AIResponse answerQuery(String naturalLanguageQuery, Long communityId);
    DefaulterPrediction predictDefaulters(Long communityId, int monthsAhead);
    CashFlowForecast forecastCashFlow(Long communityId, int monthsAhead);
    FinancialHealthScore calculateHealthScore(Long communityId);
    List<AIInsight> generateInsights(Long communityId);
    BillExplanation explainBill(Long invoiceId);
}
```

Integrates with existing AI agent tool-calling pattern (BillingQueryTools). Adds CFBOS-specific tools.

### 4.16 Auto-Accounting Rules

Every financial transaction automatically creates journal entries:

| Event | Debit | Credit |
|---|---|---|
| Invoice generated | Resident Receivable | Income (per charge head) + CGST/SGST Payable |
| Payment received | Bank Account | Resident Receivable |
| Overpayment | Bank Account | Advance from Residents (Wallet) |
| Expense recorded | Expense Account + GST Input Credit | Payable/Bank |
| Vendor payment | Vendor Payable | Bank + TDS Payable (if applicable) |
| Fund contribution | Fund Receivable | Fund Income |
| Fund utilization | Fund Expense | Fund Balance |
| Penalty applied | Resident Receivable | Penalty Income |
| Security deposit | Bank Account | Security Deposit Liability |
| Depreciation | Depreciation Expense | Accumulated Depreciation |

---

## 5. REST API Design

### 5.1 API Conventions

| Convention | Rule |
|---|---|
| Base path | `/api/cfbos/v1/{module}` |
| Auth | Bearer JWT (existing JwtTokenProvider) |
| Tenant resolution | From JWT claims → schema switch |
| Pagination | `?page=0&size=20&sort=createdAt,desc` (Spring Pageable) |
| Filtering | JPA Specifications via query params |
| Money format | String `"6200.00"` (avoids floating-point) |
| Response envelope | `{ success, data, message, timestamp, path }` |
| Error format | `{ success: false, error: { code, message, details[], field } }` |

### 5.2 Endpoint Catalog (~180 endpoints)

#### Billing (`/api/cfbos/v1/billing`) — 17 endpoints
- `POST /runs` — Execute billing run
- `POST /runs/preview` — Preview billing run (dry run)
- `GET /runs` — List billing runs
- `GET /runs/{id}` — Get billing run details
- `POST /runs/{id}/supplementary` — Supplementary run
- `GET|POST /categories` — List/create billing categories
- `PUT /categories/{id}` — Update category
- `GET|POST /rules` — List/create billing rules
- `PUT|DELETE /rules/{id}` — Update/delete rule
- `GET /rules/{id}/history` — Rule history
- `GET|POST /charge-types` — List/create charge types
- `GET|POST /rate-cards` — List/create rate cards
- `PUT /rate-cards/{id}` — Update rate card
- `GET|PUT /property/{propertyId}/config` — Property billing config
- `GET /property/{propertyId}/preview` — Preview charges

#### Invoices (`/api/cfbos/v1/invoices`) — 22 endpoints
- CRUD for invoices, credit notes, debit notes
- `POST /{id}/send` — Send invoice
- `POST /{id}/cancel` — Cancel invoice
- `GET /{id}/pdf` — Download PDF
- `GET /{id}/history` — Version history
- `POST /bulk` — Bulk generation
- `GET /outstanding`, `GET /overdue` — Summary views
- CRUD for templates, recurring schedules, adjustments

#### Charges (`/api/cfbos/v1/charges`) — 14 endpoints
- `POST /calculate` — Ad-hoc calculation
- CRUD for formulas, slabs, waivers, exemptions, meters, readings

#### Payments (`/api/cfbos/v1/payments`) — 21 endpoints
- `POST /` — Record payment
- `POST /{id}/allocate` — Manual allocation
- `POST /{id}/auto-allocate` — Auto FIFO allocation
- `POST /bulk` — Bulk payments
- `POST /gateway/initiate|callback` — Gateway integration
- CRUD for receipts, refunds, reminders, mandates, installment plans

#### Wallet (`/api/cfbos/v1/wallets`) — 9 endpoints
- `GET /me` — My wallet
- `GET|POST /{residentId}/transactions|credit|adjust` — Wallet operations
- CRUD for security deposits

#### Penalties (`/api/cfbos/v1/penalties`) — 8 endpoints
- Config CRUD, calculate, apply, waivers, interest accruals

#### Accounting (`/api/cfbos/v1/accounting`) — 20 endpoints
- Chart of accounts CRUD (tree view)
- Journal entry CRUD, post, reverse
- Trial balance, income statement, balance sheet, cash flow
- Sub-ledger queries (resident, vendor)
- Fiscal year and period management, day book

#### Expenses (`/api/cfbos/v1/expenses`) — 11 endpoints
- CRUD, submit for approval, categories, recurring, analytics

#### Vendors (`/api/cfbos/v1/vendors`) — 22 endpoints
- Vendor CRUD, ledger, ageing
- Purchase order CRUD, approve
- Vendor invoice CRUD, approve
- Vendor payment CRUD
- GRN, advances, contracts

#### Funds (`/api/cfbos/v1/funds`) — 8 endpoints
- CRUD, transfers, utilizations, statements, rules

#### Budgets (`/api/cfbos/v1/budgets`) — 9 endpoints
- CRUD, submit, revisions, variance, actuals, dashboard

#### Treasury (`/api/cfbos/v1/treasury`) — 18 endpoints
- Bank accounts, statement import, reconciliation (auto/manual/finalize)
- Cheques, cash book, payment advice, FDs, petty cash

#### Tax (`/api/cfbos/v1/tax`) — 16 endpoints
- GST config, rates, calculate, returns (GSTR-1, GSTR-3B), input credit, output liability
- HSN/SAC master, TDS sections/rates/deductions/certificates/returns/challans

#### Reports (`/api/cfbos/v1/reports`) — 15 endpoints
- All financial statements and operational reports
- PDF/Excel export, schedule, archives

#### Resident Portal (`/api/cfbos/v1/resident`) — 14 endpoints
- Dashboard, my invoices/payments/wallet/statement
- PDF downloads, disputes, queries, auto-debit, pay, installment plans

#### Committee Portal (`/api/cfbos/v1/committee`) — 10 endpoints
- Dashboard, collection/outstanding/cash/fund/budget/expense/vendor summaries
- Pending approvals, health score

#### Approvals (`/api/cfbos/v1/approvals`) — 6 endpoints
- Pending, detail, approve, reject, history, workflow config

#### AI (`/api/cfbos/v1/ai`) — 9 endpoints
- Natural language query, bill explanation
- Predictions (defaulters, cash flow, budget)
- Health score, insights, optimizations, query log

#### Analytics (`/api/cfbos/v1/analytics`) — 10 endpoints
- Collection, recovery, cash flow, expense, fund, vendor, budget, resident behaviour, community health, KPIs

#### Automation (`/api/cfbos/v1/automation`) — 9 endpoints
- CRUD for rules, schedules; list/retry runs

#### Admin (`/api/cfbos/v1/admin`) — 6 endpoints
- Audit trail, financial logs, maker-checker config, CFBOS config

---

## 6. Frontend Architecture

### 6.1 Technology Stack

React 19 + TypeScript + Vite, Tailwind CSS 4 + shadcn/ui + Radix UI, MUI 9 DataGrid, Recharts, React Hook Form + Zod, TanStack Query, React Router.

### 6.2 Portal Routes (7 portals, role-gated)

```
/finance/resident/       — Resident Portal (MODULE 16)
/finance/committee/      — Committee Portal (MODULE 17)
/finance/accountant/     — Accountant Portal
/finance/treasurer/      — Treasurer Portal
/finance/auditor/        — Auditor Portal (read-only)
/finance/vendor-portal/  — Vendor Self-Service
/finance/admin/          — Admin Portal
/finance/analytics/      — Analytics Dashboard (MODULE 20)
/finance/ai/             — AI Dashboard (MODULE 19)
```

### 6.3 Key Reusable Components

- MoneyDisplay — formatted INR with color coding
- InvoiceStatusBadge — color-coded status chips
- PaymentMethodIcon — UPI/Card/NEFT/Cash icons
- AccountTree — recursive CoA tree component
- LedgerTable — debit/credit with running balance
- DateRangePicker — fiscal year-aware
- ApprovalActions — approve/reject with comments
- DocumentPdf — in-app PDF viewer
- FinanceSearch — unified search
- AmountInput — INR auto-formatting
- TaxBreakdown — CGST/SGST/IGST display
- AgingChart — receivable aging bars

### 6.4 Dashboard Widgets

- CollectionGauge — % collected (circular)
- OutstandingTicker — total outstanding + trend
- CashPositionCard — bank balances
- FundBalanceCards — fund cards
- BudgetUtilizationBar — spent vs allocated
- RecentPaymentsFeed — live feed
- OverdueAlertBanner — top-bar alert
- DefaulterHeatmap — tower/block visualization
- ExpensePieChart — by category
- CashFlowChart — inflow vs outflow

---

## 7. Security Architecture

### 7.1 Authentication & Authorization

- Layer 1: JWT authentication (existing JwtTokenProvider)
- Layer 2: RBAC with CFBOS-specific roles: CFBOS_RESIDENT, CFBOS_ACCOUNTANT, CFBOS_TREASURER, CFBOS_AUDITOR, CFBOS_COMMITTEE, CFBOS_VENDOR, CFBOS_ADMIN
- Layer 3: Data-level security — residents/vendors see only their own data, enforced via @PreAuthorize + JPA Specifications

### 7.2 Maker-Checker

Configurable per operation. Always required for: vendor payment >50K, fund transfer, penalty waiver, refund >10K, manual journal entry. Configurable for: expense approval (threshold-based), invoice cancellation, write-off.

### 7.3 Immutable Audit Trail

Append-only `cfbos_financial_audit_trail` table. JPA @EntityListener on all entities. Checksum chain (SHA-256) for tamper detection. Partitioned quarterly.

### 7.4 Data Protection

Vendor bank account numbers and cheque numbers encrypted via existing FieldEncryptionService (AES-256). API-level data masking by role.

---

## 8. Automation & Scheduling

### 8.1 Scheduled Jobs

| Job | Schedule | Action |
|---|---|---|
| MonthlyBillingJob | 1st of month, 00:30 | Execute billing run |
| RecurringInvoiceJob | Daily 01:00 | Generate from templates |
| OverdueMarkerJob | Daily 06:00 | Mark invoices OVERDUE |
| PenaltyCalculationJob | Daily 06:30 | Calculate & apply penalties |
| InterestAccrualJob | Daily 07:00 | Accrue interest |
| PaymentReminderJob | Daily 09:00 | Send reminders |
| AutoDebitJob | Due date, 10:00 | Process auto-debit |
| ReconciliationJob | Daily 23:00 | Auto-reconcile |
| BudgetAlertJob | Weekly Monday 08:00 | Check thresholds |
| ReportGenerationJob | Monthly 1st, 02:00 | Generate reports |
| FinancialHealthJob | Weekly Sunday 22:00 | Calculate health score |
| DataArchivalJob | Quarterly | Archive to history tables |

### 8.2 Notification Triggers

Invoice generated → Email (PDF) + SMS + Push. Payment due → reminders at -3, -1, 0, +1, +7, +15, +30 days. Payment received → Email (receipt) + Push. Penalty applied → Email + Push. Approval required → Email + Push + In-App badge.

---

## 9. Integration Architecture

### 9.1 Internal Integration

CFBOS references existing entities by FK (resident_id → app_user.id, property_id → community property). No data duplication. Cross-module events: BookingConfirmedEvent → BillingEngine, OrderCompletedEvent → InvoiceEngine.

### 9.2 External Integration

Payment gateways via Strategy pattern: PaymentGatewayAdapter interface with RazorpayAdapter, PayUAdapter, ManualPaymentAdapter. Bank statement import via BankStatementParser interface with bank-specific parsers (CSV, OFX, MT940). Notification via existing AWS SES + Twilio.

---

## 10. Future Microservices Path

Vertical packages enable clean extraction:
- Phase 1: billing-service, payment-service, invoice-service
- Phase 2: accounting-service, vendor-service, treasury-service
- Phase 3: reporting-service, ai-finance-service, analytics-service

Inter-service: Spring Cloud + Kafka (replace ApplicationEvents), API Gateway, shared-nothing architecture.
