-- Sales targets per agent per month
CREATE TABLE cpos_crm_sales_target (
    id                      BIGSERIAL PRIMARY KEY,
    community_id            BIGINT      NOT NULL,
    agent_name              VARCHAR(255) NOT NULL,
    period_month            SMALLINT    NOT NULL,
    period_year             SMALLINT    NOT NULL,
    target_leads_closed     INT         NOT NULL DEFAULT 0,
    target_revenue_lakhs    INT         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sales_target_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT uq_sales_target UNIQUE (community_id, agent_name, period_month, period_year)
);
