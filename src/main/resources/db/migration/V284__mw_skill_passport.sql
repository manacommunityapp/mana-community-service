-- V284: Skill Passport
CREATE TABLE mw_skill_passports (
  id                BIGSERIAL PRIMARY KEY,
  mana_user_id      BIGINT NOT NULL UNIQUE,
  is_public         BOOLEAN DEFAULT FALSE,
  bio               TEXT,
  location          VARCHAR(200),
  preferred_roles   VARCHAR(500),
  availability      VARCHAR(30) DEFAULT 'UNAVAILABLE',
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_passport_user ON mw_skill_passports(mana_user_id);

CREATE TABLE mw_passport_skills (
  id                BIGSERIAL PRIMARY KEY,
  passport_id       BIGINT NOT NULL,
  skill_name        VARCHAR(100) NOT NULL,
  category          VARCHAR(100),
  proficiency       VARCHAR(20) DEFAULT 'INTERMEDIATE',
  years_exp         DECIMAL(4,1),
  is_verified       BOOLEAN DEFAULT FALSE,
  verified_by       VARCHAR(100),
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (passport_id) REFERENCES mw_skill_passports(id) ON DELETE CASCADE
);

CREATE TABLE mw_passport_certifications (
  id                BIGSERIAL PRIMARY KEY,
  passport_id       BIGINT NOT NULL,
  name              VARCHAR(200) NOT NULL,
  issuing_body      VARCHAR(200),
  issue_date        DATE,
  expiry_date       DATE,
  credential_url    VARCHAR(500),
  is_verified       BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (passport_id) REFERENCES mw_skill_passports(id) ON DELETE CASCADE
);

CREATE TABLE mw_passport_work_history (
  id                BIGSERIAL PRIMARY KEY,
  passport_id       BIGINT NOT NULL,
  employer_name     VARCHAR(200) NOT NULL,
  role              VARCHAR(200),
  start_date        DATE,
  end_date          DATE,
  is_current        BOOLEAN DEFAULT FALSE,
  is_mana_verified  BOOLEAN DEFAULT FALSE,
  hr_employee_id    BIGINT,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (passport_id) REFERENCES mw_skill_passports(id) ON DELETE CASCADE
);
