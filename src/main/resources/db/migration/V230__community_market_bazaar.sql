CREATE TABLE market_event_bazaar (
    id BIGINT PRIMARY KEY REFERENCES event_community(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'PUBLISHED',
    banner_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_market_event_bazaar_status ON market_event_bazaar(status);

CREATE TABLE market_stall (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES market_event_bazaar(id) ON DELETE CASCADE,
    stall_number VARCHAR(40) NOT NULL,
    zone VARCHAR(100),
    package_name VARCHAR(100) NOT NULL,
    description TEXT,
    size VARCHAR(100),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 1.00),
    currency VARCHAR(5) NOT NULL DEFAULT 'INR',
    facilities TEXT,
    allowed_categories TEXT,
    floor_row INTEGER CHECK (floor_row IS NULL OR floor_row > 0),
    floor_column INTEGER CHECK (floor_column IS NULL OR floor_column > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_market_stall_number UNIQUE(event_id, stall_number)
);
CREATE INDEX idx_market_stall_event_status ON market_stall(event_id, status);

CREATE TABLE market_application (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES market_event_bazaar(id) ON DELETE CASCADE,
    stall_id BIGINT NOT NULL REFERENCES market_stall(id) ON DELETE CASCADE,
    applicant_id BIGINT NOT NULL REFERENCES app_user(id),
    vendor_name VARCHAR(150) NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    products TEXT NOT NULL,
    compliance_documents TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason VARCHAR(1000),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_market_application_vendor ON market_application(applicant_id, requested_at DESC);
CREATE INDEX idx_market_application_event_status ON market_application(event_id, status);

CREATE TABLE market_booking (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE REFERENCES market_application(id) ON DELETE CASCADE,
    stall_id BIGINT NOT NULL REFERENCES market_stall(id) ON DELETE CASCADE,
    applicant_id BIGINT NOT NULL REFERENCES app_user(id),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(5) NOT NULL DEFAULT 'INR',
    status VARCHAR(24) NOT NULL DEFAULT 'PAYMENT_PENDING',
    hold_expires_at TIMESTAMP NOT NULL,
    payment_order_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_market_booking_status_expiry ON market_booking(status, hold_expires_at);
CREATE INDEX idx_market_booking_stall_status ON market_booking(stall_id, status);





