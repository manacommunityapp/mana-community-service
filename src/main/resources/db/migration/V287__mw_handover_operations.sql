-- V287: Duty Handover records and Operational Assignment tracking

CREATE TABLE mw_duty_handovers (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id      BIGINT NOT NULL,
    shift_assignment_id  BIGINT,
    handover_from_id     BIGINT,
    handover_to_id       BIGINT,
    location             VARCHAR(200),
    notes                TEXT,
    pending_tasks        TEXT,
    equipment_status     VARCHAR(500),
    incident_flag        BOOLEAN DEFAULT FALSE,
    incident_desc        TEXT,
    acknowledged         BOOLEAN DEFAULT FALSE,
    acknowledged_at      TIMESTAMP,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_handover_org (organization_id),
    INDEX idx_handover_to (handover_to_id),
    FOREIGN KEY (handover_from_id) REFERENCES hr_employees(id),
    FOREIGN KEY (handover_to_id)   REFERENCES hr_employees(id)
);

CREATE TABLE mw_operational_assignments (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id   BIGINT NOT NULL,
    source_module     VARCHAR(50) NOT NULL,
    source_ref_id     BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    assignment_type   VARCHAR(100),
    status            VARCHAR(30) DEFAULT 'ASSIGNED',
    assigned_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    incentive_amount  DECIMAL(10,2),
    performance_score INT,
    notes             TEXT,
    INDEX idx_op_assign_org (organization_id),
    INDEX idx_op_assign_employee (employee_id),
    INDEX idx_op_assign_source (source_module, source_ref_id),
    FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);
