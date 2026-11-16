-- V242: HRM Employee Management (employment history, onboarding, offboarding, org hierarchy)

CREATE TABLE hr_employment_history (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id             BIGINT NOT NULL,
    employee_id                 BIGINT NOT NULL,
    change_type                 VARCHAR(50) NOT NULL,
    effective_date              DATE NOT NULL,
    previous_designation        VARCHAR(100),
    new_designation             VARCHAR(100),
    previous_department         VARCHAR(100),
    new_department              VARCHAR(100),
    previous_branch             VARCHAR(100),
    new_branch                  VARCHAR(100),
    previous_reporting_manager_id BIGINT,
    new_reporting_manager_id    BIGINT,
    reason                      VARCHAR(500),
    remarks                     TEXT,
    changed_by                  BIGINT,
    created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_history_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id),
    INDEX idx_emp_history_employee (employee_id),
    INDEX idx_emp_history_org (organization_id),
    INDEX idx_emp_history_date (effective_date)
);

CREATE TABLE hr_onboarding_templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    template_name   VARCHAR(200) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_onboarding_tmpl_org (organization_id)
);

CREATE TABLE hr_onboarding_tasks (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    employee_id      BIGINT NOT NULL,
    template_id      BIGINT,
    task_title       VARCHAR(300) NOT NULL,
    task_description TEXT,
    assigned_to      BIGINT,
    due_date         DATE,
    is_completed     BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at     DATETIME,
    completed_by     BIGINT,
    category         VARCHAR(100),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_onboarding_task_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id),
    INDEX idx_onboarding_task_employee (employee_id),
    INDEX idx_onboarding_task_org (organization_id)
);

CREATE TABLE hr_offboarding_tasks (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    employee_id      BIGINT NOT NULL,
    template_id      BIGINT,
    task_title       VARCHAR(300) NOT NULL,
    task_description TEXT,
    assigned_to      BIGINT,
    due_date         DATE,
    is_completed     BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at     DATETIME,
    completed_by     BIGINT,
    category         VARCHAR(100),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offboarding_task_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id),
    INDEX idx_offboarding_task_employee (employee_id),
    INDEX idx_offboarding_task_org (organization_id)
);
