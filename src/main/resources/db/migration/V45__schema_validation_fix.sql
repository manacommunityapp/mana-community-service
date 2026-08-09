-- Migration V45: Schema validation sync for event_gallery_item, event_department, invoice_category, and email_theme

-- 1. Ensure event_gallery_item has missing columns (day_tag, category)
ALTER TABLE IF EXISTS manacommunity.event_gallery_item
  ADD COLUMN IF NOT EXISTS day_tag  VARCHAR(100),
  ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- 2. Create event_department table if not exists
CREATE TABLE IF NOT EXISTS manacommunity.event_department (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT,
    event_id BIGINT,
    name VARCHAR(120) NOT NULL,
    head_name VARCHAR(120),
    total_target INT NOT NULL DEFAULT 10,
    present_count INT NOT NULL DEFAULT 0,
    color VARCHAR(30) DEFAULT '#6366f1',
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Create invoice_category table if not exists
CREATE TABLE IF NOT EXISTS manacommunity.invoice_category (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(60),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 4. Create email_theme table if not exists
CREATE TABLE IF NOT EXISTS manacommunity.email_theme (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    theme_json TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 5. Add theme_json column to email_builder_template if not exists
ALTER TABLE IF EXISTS manacommunity.email_builder_template
  ADD COLUMN IF NOT EXISTS theme_json TEXT;
