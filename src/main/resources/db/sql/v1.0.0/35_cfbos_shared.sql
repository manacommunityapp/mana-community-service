-- src/main/resources/db/sql/v1.0.0/35_cfbos_shared.sql

-- Document sequence generator
CREATE TABLE IF NOT EXISTS cfbos_document_sequence (
    id BIGSERIAL PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    fiscal_year VARCHAR(9) NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    padding_length INTEGER NOT NULL DEFAULT 6,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(document_type, fiscal_year)
);

-- CFBOS module configuration
CREATE TABLE IF NOT EXISTS cfbos_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    config_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(255),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
