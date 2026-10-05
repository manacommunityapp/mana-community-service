-- Society Finance: expense vouchers with maker-checker-approver workflow
CREATE TABLE IF NOT EXISTS society_expense (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id    BIGINT         NOT NULL,
    voucher_number  VARCHAR(50),
    category        VARCHAR(100)   NOT NULL,
    title           VARCHAR(255)   NOT NULL,
    description     VARCHAR(2000),
    amount          DECIMAL(14,2)  NOT NULL,
    account_id      VARCHAR(50),
    account_name    VARCHAR(255),
    vendor_id       VARCHAR(50),
    vendor_name     VARCHAR(255),
    status          VARCHAR(30)    NOT NULL DEFAULT 'PENDING_CHECKER',
    maker_user_id   BIGINT,
    maker_name      VARCHAR(255),
    maker_date      DATE,
    checker_user_id BIGINT,
    checker_name    VARCHAR(255),
    checker_date    DATE,
    checker_notes   VARCHAR(1000),
    approver_user_id BIGINT,
    approver_name   VARCHAR(255),
    approver_date   DATE,
    approver_notes  VARCHAR(1000),
    receipt_url     VARCHAR(500),
    payment_method  VARCHAR(50),
    utr_reference   VARCHAR(100),
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME,
    INDEX idx_society_expense_community (community_id),
    INDEX idx_society_expense_status (status)
);

-- Society Finance: chart of accounts
CREATE TABLE IF NOT EXISTS chart_of_account (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id    BIGINT        NOT NULL,
    code            VARCHAR(20)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    balance         DECIMAL(14,2) NOT NULL DEFAULT 0,
    description     VARCHAR(500),
    is_reserve_fund BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME,
    INDEX idx_coa_community (community_id)
);

-- Society Finance: general ledger (double-entry bookkeeping)
CREATE TABLE IF NOT EXISTS general_ledger_entry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id    BIGINT        NOT NULL,
    entry_date      DATE          NOT NULL,
    voucher_number  VARCHAR(50),
    account_code    VARCHAR(20)   NOT NULL,
    account_name    VARCHAR(255),
    debit           DECIMAL(14,2) NOT NULL DEFAULT 0,
    credit          DECIMAL(14,2) NOT NULL DEFAULT 0,
    description     VARCHAR(500),
    reference_type  VARCHAR(50),
    reference_id    BIGINT,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_gle_community (community_id),
    INDEX idx_gle_entry_date (entry_date)
);

-- Safety: security incidents
CREATE TABLE IF NOT EXISTS security_incident (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id      BIGINT       NOT NULL,
    type              VARCHAR(50)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    description       VARCHAR(2000),
    status            VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    priority          VARCHAR(30),
    location          VARCHAR(500),
    reported_by_id    BIGINT,
    reported_by_name  VARCHAR(255),
    assigned_to_id    BIGINT,
    assigned_to_name  VARCHAR(255),
    image_url         VARCHAR(500),
    resolution_notes  VARCHAR(2000),
    resolved_at       DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME,
    INDEX idx_security_incident_community (community_id),
    INDEX idx_security_incident_status (status)
);
