CREATE TABLE IF NOT EXISTS interior_saved_inspiration (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    service_request_id BIGINT REFERENCES service_request(id) ON DELETE SET NULL,
    source_url TEXT NOT NULL,
    image_url TEXT,
    title VARCHAR(240),
    notes TEXT,
    tags TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_interior_inspiration_user_url UNIQUE(user_id, source_url)
);
CREATE INDEX IF NOT EXISTS idx_interior_inspiration_user ON interior_saved_inspiration(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS interior_change_request (
    id BIGSERIAL PRIMARY KEY,
    service_request_id BIGINT NOT NULL REFERENCES service_request(id) ON DELETE CASCADE,
    proposed_by BIGINT NOT NULL REFERENCES app_user(id),
    description TEXT NOT NULL,
    scope_delta TEXT,
    price_delta NUMERIC(12,2),
    proposed_start_date DATE,
    proposed_end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_note TEXT,
    decided_by BIGINT REFERENCES app_user(id),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_interior_change_project ON interior_change_request(service_request_id, created_at DESC);

CREATE TABLE IF NOT EXISTS interior_payment_milestone (
    id BIGSERIAL PRIMARY KEY,
    service_request_id BIGINT NOT NULL REFERENCES service_request(id) ON DELETE CASCADE,
    provider_id BIGINT NOT NULL REFERENCES service_provider(id),
    title VARCHAR(180) NOT NULL,
    description TEXT,
    amount NUMERIC(12,2) NOT NULL CHECK(amount > 0),
    due_date DATE,
    status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED',
    payment_gateway VARCHAR(24),
    razorpay_order_id VARCHAR(60),
    razorpay_payment_id VARCHAR(60),
    cashfree_mana_order_id VARCHAR(60),
    authorized_at TIMESTAMP,
    released_at TIMESTAMP,
    payout_reference VARCHAR(120),
    invoice_reference VARCHAR(120),
    invoice_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_interior_payment_project ON interior_payment_milestone(service_request_id, due_date, id);
CREATE TABLE IF NOT EXISTS interior_payment_event (
    id BIGSERIAL PRIMARY KEY,
    milestone_id BIGINT NOT NULL REFERENCES interior_payment_milestone(id) ON DELETE CASCADE,
    actor_user_id BIGINT NOT NULL REFERENCES app_user(id),
    event_type VARCHAR(32) NOT NULL,
    reference TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE service_provider ADD COLUMN IF NOT EXISTS verified_credential_level VARCHAR(40);
ALTER TABLE service_provider ADD COLUMN IF NOT EXISTS credentials_verified_at TIMESTAMP;
CREATE TABLE IF NOT EXISTS service_provider_credential (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES service_provider(id) ON DELETE CASCADE,
    credential_type VARCHAR(80) NOT NULL,
    credential_level VARCHAR(40) NOT NULL,
    issuer VARCHAR(180),
    credential_number VARCHAR(120),
    evidence_media_id UUID NOT NULL REFERENCES manacommunity.media_objects(external_id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_by BIGINT NOT NULL REFERENCES app_user(id),
    reviewed_by BIGINT REFERENCES app_user(id),
    review_note TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_provider_credential_provider ON service_provider_credential(provider_id, created_at DESC);
CREATE TABLE IF NOT EXISTS service_provider_credential_audit (
    id BIGSERIAL PRIMARY KEY,
    credential_id BIGINT NOT NULL REFERENCES service_provider_credential(id) ON DELETE CASCADE,
    actor_user_id BIGINT NOT NULL REFERENCES app_user(id),
    action VARCHAR(24) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sp_service_quote_line_item
    ADD COLUMN IF NOT EXISTS catalog_product_id BIGINT REFERENCES commerce_products(id),
    ADD COLUMN IF NOT EXISTS procurement_quantity INTEGER;
CREATE INDEX IF NOT EXISTS idx_interior_quote_line_product ON sp_service_quote_line_item(catalog_product_id);
