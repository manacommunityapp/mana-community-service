-- HRM Security, Compliance and Analytics tables

CREATE TABLE hr_employee_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    granted_by BIGINT,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uq_emp_role (organization_id, employee_id, role),
    FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);

CREATE TABLE hr_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_org (organization_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_actor (actor_user_id)
);

CREATE TABLE hr_compliance_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    INDEX idx_compliance_org (organization_id),
    INDEX idx_compliance_due (due_date),
    FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);

CREATE TABLE hr_report_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    filters_json TEXT,
    columns_json TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
