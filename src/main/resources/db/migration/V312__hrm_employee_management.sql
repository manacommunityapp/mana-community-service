-- V312: HRM Employee Management (employment history, onboarding, offboarding)
CREATE TABLE IF NOT EXISTS hr_employment_history (
    id                            BIGSERIAL PRIMARY KEY,
    organization_id               BIGINT NOT NULL,
    employee_id                   BIGINT NOT NULL,
    change_type                   VARCHAR(50) NOT NULL,
    effective_date                DATE NOT NULL,
    previous_designation          VARCHAR(100),
    new_designation               VARCHAR(100),
    previous_department           VARCHAR(100),
    new_department                VARCHAR(100),
    previous_branch               VARCHAR(100),
    new_branch                    VARCHAR(100),
    previous_reporting_manager_id BIGINT,
    new_reporting_manager_id      BIGINT,
    reason                        VARCHAR(500),
    remarks                       TEXT,
    changed_by                    BIGINT,
    created_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_history_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);
CREATE INDEX IF NOT EXISTS idx_emp_history_employee ON hr_employment_history(employee_id);
CREATE INDEX IF NOT EXISTS idx_emp_history_org ON hr_employment_history(organization_id);
CREATE INDEX IF NOT EXISTS idx_emp_history_date ON hr_employment_history(effective_date);

CREATE TABLE IF NOT EXISTS hr_onboarding_templates (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    template_name   VARCHAR(200) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_onboarding_tmpl_org ON hr_onboarding_templates(organization_id);

CREATE TABLE IF NOT EXISTS hr_onboarding_tasks (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    employee_id      BIGINT NOT NULL,
    template_id      BIGINT,
    task_title       VARCHAR(300) NOT NULL,
    task_description TEXT,
    assigned_to      BIGINT,
    due_date         DATE,
    is_completed     BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at     TIMESTAMP,
    completed_by     BIGINT,
    category         VARCHAR(100),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_onboarding_task_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);
CREATE INDEX IF NOT EXISTS idx_onboarding_task_employee ON hr_onboarding_tasks(employee_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_task_org ON hr_onboarding_tasks(organization_id);

CREATE TABLE IF NOT EXISTS hr_offboarding_tasks (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    employee_id      BIGINT NOT NULL,
    template_id      BIGINT,
    task_title       VARCHAR(300) NOT NULL,
    task_description TEXT,
    assigned_to      BIGINT,
    due_date         DATE,
    is_completed     BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at     TIMESTAMP,
    completed_by     BIGINT,
    category         VARCHAR(100),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offboarding_task_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id)
);
CREATE INDEX IF NOT EXISTS idx_offboarding_task_employee ON hr_offboarding_tasks(employee_id);
CREATE INDEX IF NOT EXISTS idx_offboarding_task_org ON hr_offboarding_tasks(organization_id);
