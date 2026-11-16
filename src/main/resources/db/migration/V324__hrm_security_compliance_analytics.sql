-- HRM Security, Compliance and Analytics tables

CREATE TABLE IF NOT EXISTS hr_employee_roles (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    granted_by BIGINT,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uq_emp_role UNIQUE (organization_id, employee_id, role),
    FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);

CREATE TABLE IF NOT EXISTS hr_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    actor_name VARCHAR(200),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    entity_description VARCHAR(500),
    before_snapshot TEXT,
    after_snapshot TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_org ON hr_audit_logs(organization_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON hr_audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON hr_audit_logs(actor_user_id);

CREATE TABLE IF NOT EXISTS hr_compliance_alerts (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    employee_id BIGINT,
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'OPEN',
    acknowledged_by BIGINT,
    acknowledged_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);
CREATE INDEX IF NOT EXISTS idx_compliance_org ON hr_compliance_alerts(organization_id);
CREATE INDEX IF NOT EXISTS idx_compliance_due ON hr_compliance_alerts(due_date);

CREATE TABLE IF NOT EXISTS hr_report_configs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    filters_json TEXT,
    columns_json TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
