-- src/main/resources/db/sql/v1.0.0/38_cfbos_charge.sql

CREATE TABLE IF NOT EXISTS cfbos_formula (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    expression TEXT NOT NULL,
    description TEXT,
    result_type VARCHAR(20) NOT NULL DEFAULT 'AMOUNT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_formula_variable (
    id BIGSERIAL PRIMARY KEY,
    formula_id BIGINT NOT NULL REFERENCES cfbos_formula(id) ON DELETE CASCADE,
    variable_name VARCHAR(50) NOT NULL,
    variable_source VARCHAR(50) NOT NULL,
    source_field VARCHAR(100) NOT NULL,
    default_value VARCHAR(50),
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS cfbos_slab_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    unit_label VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_tier_config (
    id BIGSERIAL PRIMARY KEY,
    slab_config_id BIGINT NOT NULL REFERENCES cfbos_slab_config(id) ON DELETE CASCADE,
    tier_from NUMERIC(18,4) NOT NULL,
    tier_to NUMERIC(18,4),
    rate NUMERIC(18,4) NOT NULL,
    fixed_charge NUMERIC(18,2) NOT NULL DEFAULT 0,
    tier_order INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cfbos_formula_variable_formula ON cfbos_formula_variable(formula_id);
CREATE INDEX IF NOT EXISTS idx_cfbos_tier_config_slab ON cfbos_tier_config(slab_config_id);
