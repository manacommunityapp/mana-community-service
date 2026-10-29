-- V205__cpos_crm_pipeline.sql
-- CRM pipeline tables for the CPOS (Community Property Operating System) module.
-- Tracks buyer/investor leads through a 6-stage sales funnel, records per-lead
-- activity, and manages broker relationships with automated commission ledger.

CREATE TABLE cpos_crm_lead (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL REFERENCES community(id),
    lead_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    source          VARCHAR(50)  NOT NULL DEFAULT 'PORTAL',
    stage           VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    lead_type       VARCHAR(50)  NOT NULL DEFAULT 'APARTMENT',
    budget_lakhs    INT          NOT NULL DEFAULT 0,
    budget_display  VARCHAR(100),
    assigned_to     VARCHAR(255),
    days_in_stage   INT          NOT NULL DEFAULT 0,
    days_total      INT          NOT NULL DEFAULT 0,
    stage_changed_at TIMESTAMPTZ DEFAULT now(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Source values: PORTAL, BROKER, WHATSAPP, WEBSITE, REFERRAL, WALK_IN
-- Stage values:  NEW, CONTACTED, QUALIFIED, SITE_VISIT, NEGOTIATION, CLOSED

CREATE TABLE cpos_crm_activity (
    id           BIGSERIAL PRIMARY KEY,
    lead_id      BIGINT      NOT NULL REFERENCES cpos_crm_lead(id),
    activity_type VARCHAR(50) NOT NULL,   -- CALL, VISIT, NOTE, OFFER, EMAIL
    note         TEXT,
    created_by   VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cpos_broker (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL REFERENCES community(id),
    broker_name     VARCHAR(255) NOT NULL,
    firm_name       VARCHAR(255),
    phone           VARCHAR(50),
    email           VARCHAR(255),
    commission_rate DECIMAL(4,2) NOT NULL DEFAULT 2.00,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE cpos_broker_commission (
    id               BIGSERIAL PRIMARY KEY,
    broker_id        BIGINT         NOT NULL REFERENCES cpos_broker(id),
    lead_id          BIGINT         NOT NULL REFERENCES cpos_crm_lead(id),
    commission_rate  DECIMAL(4,2)   NOT NULL,
    commission_lakhs DECIMAL(10,2)  NOT NULL,
    status           VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    -- status: PENDING, APPROVED, PAID
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_cpos_crm_lead_community  ON cpos_crm_lead(community_id);
CREATE INDEX idx_cpos_crm_lead_stage      ON cpos_crm_lead(stage);
CREATE INDEX idx_cpos_crm_lead_assigned   ON cpos_crm_lead(assigned_to);
CREATE INDEX idx_cpos_crm_activity_lead   ON cpos_crm_activity(lead_id);
CREATE INDEX idx_cpos_broker_community    ON cpos_broker(community_id);
CREATE INDEX idx_cpos_broker_comm_broker  ON cpos_broker_commission(broker_id);
CREATE INDEX idx_cpos_broker_comm_lead    ON cpos_broker_commission(lead_id);
CREATE INDEX idx_cpos_broker_comm_status  ON cpos_broker_commission(status);
