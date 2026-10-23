-- Procurement Hub: Supplier Marketplace + RFQ + Quotes + Purchase Orders

-- Supplier Profiles
CREATE TABLE supplier_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    business_name       VARCHAR(160)   NOT NULL,
    contact_person      VARCHAR(120),
    mobile              VARCHAR(20),
    email               VARCHAR(120),
    gst_number          VARCHAR(30),
    pan                 VARCHAR(20),
    address             VARCHAR(400),
    city                VARCHAR(80),
    pincode             VARCHAR(10),
    categories          VARCHAR(500),
    service_area        VARCHAR(500),
    verification_status VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    rating              NUMERIC(3,2)   NOT NULL DEFAULT 0,
    review_count        INTEGER        NOT NULL DEFAULT 0,
    fulfillment_rate    NUMERIC(5,2)   NOT NULL DEFAULT 100,
    on_time_rate        NUMERIC(5,2)   NOT NULL DEFAULT 100,
    total_orders        INTEGER        NOT NULL DEFAULT 0,
    description         VARCHAR(800),
    logo_url            VARCHAR(500),
    certifications      VARCHAR(500),
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    community_id        BIGINT,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_supplier_profiles_city         ON supplier_profiles(city);
CREATE INDEX idx_supplier_profiles_verification ON supplier_profiles(verification_status);
CREATE INDEX idx_supplier_profiles_community    ON supplier_profiles(community_id);
CREATE INDEX idx_supplier_profiles_active       ON supplier_profiles(active);

-- Supplier Products / Catalogue
CREATE TABLE supplier_products (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT         NOT NULL REFERENCES supplier_profiles(id),
    name            VARCHAR(200)   NOT NULL,
    description     VARCHAR(800),
    category        VARCHAR(40)    NOT NULL,
    unit            VARCHAR(40)    NOT NULL,
    wholesale_price NUMERIC(12,2)  NOT NULL,
    retail_price    NUMERIC(12,2),
    moq             INTEGER        NOT NULL DEFAULT 1,
    max_order_qty   INTEGER,
    gst_percent     NUMERIC(5,2)   NOT NULL DEFAULT 18,
    lead_time_days  INTEGER        NOT NULL DEFAULT 3,
    in_stock        BOOLEAN        NOT NULL DEFAULT TRUE,
    image_url       VARCHAR(500),
    specifications  VARCHAR(1000),
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_supplier_products_supplier  ON supplier_products(supplier_id);
CREATE INDEX idx_supplier_products_category  ON supplier_products(category);
CREATE INDEX idx_supplier_products_active    ON supplier_products(active);

-- RFQs
CREATE TABLE procurement_rfqs (
    id                  BIGSERIAL PRIMARY KEY,
    rfq_number          VARCHAR(30)    NOT NULL UNIQUE,
    title               VARCHAR(200)   NOT NULL,
    description         VARCHAR(1000),
    status              VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    community_id        BIGINT         NOT NULL,
    requested_by        VARCHAR(120)   NOT NULL,
    delivery_location   VARCHAR(300)   NOT NULL,
    delivery_deadline   DATE           NOT NULL,
    quotation_deadline  DATE           NOT NULL,
    estimated_value     NUMERIC(14,2),
    approved_budget     NUMERIC(14,2),
    invited_supplier_ids VARCHAR(500),
    quotes_received     INTEGER        NOT NULL DEFAULT 0,
    notes               VARCHAR(800),
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_procurement_rfqs_community ON procurement_rfqs(community_id);
CREATE INDEX idx_procurement_rfqs_status    ON procurement_rfqs(status);

-- RFQ Line Items
CREATE TABLE rfq_items (
    id                    BIGSERIAL PRIMARY KEY,
    rfq_id                BIGINT        NOT NULL REFERENCES procurement_rfqs(id) ON DELETE CASCADE,
    product_name          VARCHAR(200)  NOT NULL,
    description           VARCHAR(600),
    category              VARCHAR(40)   NOT NULL DEFAULT 'OTHER',
    unit                  VARCHAR(40)   NOT NULL DEFAULT 'units',
    quantity              INTEGER       NOT NULL,
    target_price_per_unit NUMERIC(12,2),
    specifications        VARCHAR(600)
);

CREATE INDEX idx_rfq_items_rfq ON rfq_items(rfq_id);

-- Supplier Quotes
CREATE TABLE supplier_quotes (
    id              BIGSERIAL PRIMARY KEY,
    rfq_id          BIGINT         NOT NULL REFERENCES procurement_rfqs(id),
    supplier_id     BIGINT         NOT NULL REFERENCES supplier_profiles(id),
    status          VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    sub_total       NUMERIC(14,2)  NOT NULL,
    gst_amount      NUMERIC(14,2)  NOT NULL,
    delivery_charge NUMERIC(10,2)  NOT NULL DEFAULT 0,
    grand_total     NUMERIC(14,2)  NOT NULL,
    valid_until     DATE           NOT NULL,
    delivery_date   DATE           NOT NULL,
    payment_terms   VARCHAR(200),
    notes           VARCHAR(800),
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    UNIQUE (rfq_id, supplier_id)
);

CREATE INDEX idx_supplier_quotes_rfq      ON supplier_quotes(rfq_id);
CREATE INDEX idx_supplier_quotes_supplier ON supplier_quotes(supplier_id);
CREATE INDEX idx_supplier_quotes_status   ON supplier_quotes(status);

-- Quote Line Items
CREATE TABLE supplier_quote_items (
    id              BIGSERIAL PRIMARY KEY,
    quote_id        BIGINT        NOT NULL REFERENCES supplier_quotes(id) ON DELETE CASCADE,
    rfq_item_id     BIGINT,
    product_name    VARCHAR(200)  NOT NULL,
    unit            VARCHAR(40)   NOT NULL DEFAULT 'units',
    quantity        INTEGER       NOT NULL,
    price_per_unit  NUMERIC(12,2) NOT NULL,
    gst_percent     NUMERIC(5,2)  NOT NULL DEFAULT 18,
    total_price     NUMERIC(14,2) NOT NULL,
    lead_time_days  INTEGER       NOT NULL DEFAULT 3,
    notes           VARCHAR(400)
);

CREATE INDEX idx_supplier_quote_items_quote ON supplier_quote_items(quote_id);

-- Purchase Orders
CREATE TABLE procurement_purchase_orders (
    id               BIGSERIAL PRIMARY KEY,
    po_number        VARCHAR(30)   NOT NULL UNIQUE,
    rfq_id           BIGINT,
    rfq_number       VARCHAR(30),
    supplier_id      BIGINT        NOT NULL REFERENCES supplier_profiles(id),
    community_id     BIGINT        NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'SENT',
    sub_total        NUMERIC(14,2) NOT NULL,
    gst_amount       NUMERIC(14,2) NOT NULL,
    delivery_charge  NUMERIC(10,2) NOT NULL DEFAULT 0,
    grand_total      NUMERIC(14,2) NOT NULL,
    delivery_deadline DATE         NOT NULL,
    delivery_address VARCHAR(400)  NOT NULL,
    payment_terms    VARCHAR(200),
    notes            VARCHAR(800),
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_procurement_po_community ON procurement_purchase_orders(community_id);
CREATE INDEX idx_procurement_po_supplier  ON procurement_purchase_orders(supplier_id);
CREATE INDEX idx_procurement_po_status    ON procurement_purchase_orders(status);

-- PO Line Items
CREATE TABLE procurement_po_items (
    id              BIGSERIAL PRIMARY KEY,
    po_id           BIGINT        NOT NULL REFERENCES procurement_purchase_orders(id) ON DELETE CASCADE,
    product_name    VARCHAR(200)  NOT NULL,
    unit            VARCHAR(40)   NOT NULL DEFAULT 'units',
    quantity        INTEGER       NOT NULL,
    price_per_unit  NUMERIC(12,2) NOT NULL,
    gst_percent     NUMERIC(5,2)  NOT NULL DEFAULT 18,
    total_price     NUMERIC(14,2) NOT NULL,
    lead_time_days  INTEGER       NOT NULL DEFAULT 3,
    notes           VARCHAR(400)
);

CREATE INDEX idx_procurement_po_items_po ON procurement_po_items(po_id);
