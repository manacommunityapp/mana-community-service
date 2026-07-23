-- FILE: db/sql/v1.0.0/36_vendor_management_system.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- VENDOR MANAGEMENT SYSTEM (VMS) — Complete Enterprise Vendor Ecosystem
-- 64 tables covering vendor lifecycle, procurement, contracts, payments,
-- work orders, ratings, performance analytics, and AI features.
-- ═══════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. VENDOR CATEGORIES & CORE
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_vendor_categories (
    id                BIGSERIAL       PRIMARY KEY,
    name              VARCHAR(120)    NOT NULL,
    slug              VARCHAR(120)    NOT NULL,
    description       VARCHAR(500),
    icon              VARCHAR(100),
    parent_id         BIGINT          REFERENCES vms_vendor_categories(id),
    sort_order        INT             NOT NULL DEFAULT 0,
    active            BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendors (
    id                    BIGSERIAL       PRIMARY KEY,
    business_name         VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    business_type         VARCHAR(40)     NOT NULL DEFAULT 'INDIVIDUAL',
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    description           VARCHAR(2000),
    logo_url              VARCHAR(500),
    cover_image_url       VARCHAR(500),
    gst_number            VARCHAR(30),
    pan                   VARCHAR(20),
    trade_license         VARCHAR(100),
    insurance_number      VARCHAR(100),
    website               VARCHAR(300),
    years_of_experience   INT             DEFAULT 0,
    number_of_employees   INT             DEFAULT 1,
    emergency_contact     VARCHAR(20),
    emergency_name        VARCHAR(120),
    average_rating        DECIMAL(3,2)    DEFAULT 0.00,
    total_ratings         INT             DEFAULT 0,
    total_jobs_completed  INT             DEFAULT 0,
    commission_rate       DECIMAL(5,2)    DEFAULT 0.00,
    verified              BOOLEAN         NOT NULL DEFAULT FALSE,
    featured              BOOLEAN         NOT NULL DEFAULT FALSE,
    vacation_mode         BOOLEAN         NOT NULL DEFAULT FALSE,
    emergency_available   BOOLEAN         NOT NULL DEFAULT FALSE,
    max_daily_jobs        INT             DEFAULT 10,
    user_id               BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    category_id           BIGINT          REFERENCES vms_vendor_categories(id),
    approved_by           BIGINT          REFERENCES app_user(id),
    approved_at           TIMESTAMP,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_owners (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    name              VARCHAR(150)    NOT NULL,
    designation       VARCHAR(100),
    email             VARCHAR(150),
    phone             VARCHAR(20),
    aadhaar           VARCHAR(20),
    pan               VARCHAR(20),
    photo_url         VARCHAR(500),
    is_primary        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_branches (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    name              VARCHAR(150)    NOT NULL,
    address           VARCHAR(500),
    city              VARCHAR(100),
    state             VARCHAR(100),
    pincode           VARCHAR(10),
    latitude          DECIMAL(10,7),
    longitude         DECIMAL(10,7),
    phone             VARCHAR(20),
    email             VARCHAR(150),
    is_head_office    BOOLEAN         NOT NULL DEFAULT FALSE,
    active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_languages (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    language          VARCHAR(50)     NOT NULL
);

CREATE TABLE IF NOT EXISTS vms_vendor_social_media (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    platform          VARCHAR(50)     NOT NULL,
    url               VARCHAR(500)    NOT NULL
);

CREATE TABLE IF NOT EXISTS vms_vendor_gallery (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    url               VARCHAR(500)    NOT NULL,
    media_type        VARCHAR(20)     NOT NULL DEFAULT 'IMAGE',
    caption           VARCHAR(300),
    sort_order        INT             NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. VENDOR REGISTRATION & ONBOARDING
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_vendor_registrations (
    id                BIGSERIAL       PRIMARY KEY,
    business_name     VARCHAR(200)    NOT NULL,
    contact_name      VARCHAR(150)    NOT NULL,
    email             VARCHAR(150)    NOT NULL,
    phone             VARCHAR(20)     NOT NULL,
    category_id       BIGINT          REFERENCES vms_vendor_categories(id),
    source            VARCHAR(30)     NOT NULL DEFAULT 'SELF',
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    notes             VARCHAR(1000),
    invited_by        BIGINT          REFERENCES app_user(id),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    vendor_id         BIGINT          REFERENCES vms_vendors(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_invitations (
    id                BIGSERIAL       PRIMARY KEY,
    email             VARCHAR(150)    NOT NULL,
    phone             VARCHAR(20),
    invitation_code   VARCHAR(50)     NOT NULL,
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    category_id       BIGINT          REFERENCES vms_vendor_categories(id),
    invited_by        BIGINT          NOT NULL REFERENCES app_user(id),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    expires_at        TIMESTAMP       NOT NULL,
    accepted_at       TIMESTAMP,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_verifications (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    type              VARCHAR(30)     NOT NULL,
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    reference_number  VARCHAR(100),
    verified_value    VARCHAR(200),
    verified_at       TIMESTAMP,
    verified_by       BIGINT          REFERENCES app_user(id),
    expiry_date       DATE,
    notes             VARCHAR(500),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_approval_steps (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    step_name         VARCHAR(100)    NOT NULL,
    step_order        INT             NOT NULL,
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    assigned_to       BIGINT          REFERENCES app_user(id),
    completed_by      BIGINT          REFERENCES app_user(id),
    completed_at      TIMESTAMP,
    comments          VARCHAR(1000),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. DOCUMENT MANAGEMENT
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_vendor_documents (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    document_type     VARCHAR(50)     NOT NULL,
    document_name     VARCHAR(200)    NOT NULL,
    file_url          VARCHAR(500)    NOT NULL,
    file_size         BIGINT,
    mime_type         VARCHAR(100),
    reference_number  VARCHAR(100),
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    issue_date        DATE,
    expiry_date       DATE,
    renewal_date      DATE,
    approved_by       BIGINT          REFERENCES app_user(id),
    approved_at       TIMESTAMP,
    version           INT             NOT NULL DEFAULT 1,
    active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_document_versions (
    id                BIGSERIAL       PRIMARY KEY,
    document_id       BIGINT          NOT NULL REFERENCES vms_vendor_documents(id) ON DELETE CASCADE,
    version           INT             NOT NULL,
    file_url          VARCHAR(500)    NOT NULL,
    file_size         BIGINT,
    uploaded_by       BIGINT          REFERENCES app_user(id),
    notes             VARCHAR(500),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. SERVICE CATALOG
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_vendor_services (
    id                    BIGSERIAL       PRIMARY KEY,
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    category_id           BIGINT          REFERENCES vms_vendor_categories(id),
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    base_price            DECIMAL(12,2)   NOT NULL DEFAULT 0,
    price_unit            VARCHAR(30)     DEFAULT 'FIXED',
    discount_percent      DECIMAL(5,2)    DEFAULT 0,
    membership_price      DECIMAL(12,2),
    emergency_price       DECIMAL(12,2),
    gst_percent           DECIMAL(5,2)    DEFAULT 0,
    duration_minutes      INT,
    warranty_days         INT             DEFAULT 0,
    cancellation_policy   VARCHAR(1000),
    required_equipment    VARCHAR(500),
    required_staff        INT             DEFAULT 1,
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    sort_order            INT             NOT NULL DEFAULT 0,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_service_images (
    id                BIGSERIAL       PRIMARY KEY,
    service_id        BIGINT          NOT NULL REFERENCES vms_vendor_services(id) ON DELETE CASCADE,
    url               VARCHAR(500)    NOT NULL,
    media_type        VARCHAR(20)     NOT NULL DEFAULT 'IMAGE',
    sort_order        INT             NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_service_areas (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    area_name         VARCHAR(150)    NOT NULL,
    city              VARCHAR(100),
    pincode           VARCHAR(10),
    radius_km         DECIMAL(6,2),
    latitude          DECIMAL(10,7),
    longitude         DECIMAL(10,7),
    active            BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS vms_vendor_service_pricing_tiers (
    id                BIGSERIAL       PRIMARY KEY,
    service_id        BIGINT          NOT NULL REFERENCES vms_vendor_services(id) ON DELETE CASCADE,
    tier_name         VARCHAR(100)    NOT NULL,
    description       VARCHAR(500),
    price             DECIMAL(12,2)   NOT NULL,
    unit              VARCHAR(30),
    min_quantity      INT             DEFAULT 1,
    max_quantity      INT,
    sort_order        INT             NOT NULL DEFAULT 0
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. VENDOR AVAILABILITY
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_vendor_working_hours (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    day_of_week       INT             NOT NULL,
    start_time        TIME            NOT NULL,
    end_time          TIME            NOT NULL,
    is_working_day    BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS vms_vendor_holidays (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    holiday_date      DATE            NOT NULL,
    name              VARCHAR(150),
    recurring         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_leaves (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    start_date        DATE            NOT NULL,
    end_date          DATE            NOT NULL,
    reason            VARCHAR(500),
    status            VARCHAR(20)     NOT NULL DEFAULT 'APPROVED',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_breaks (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    day_of_week       INT             NOT NULL,
    start_time        TIME            NOT NULL,
    end_time          TIME            NOT NULL
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 6. BOOKINGS & MARKETPLACE
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_bookings (
    id                    BIGSERIAL       PRIMARY KEY,
    booking_number        VARCHAR(30)     NOT NULL,
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    resident_id           BIGINT          NOT NULL REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    booking_type          VARCHAR(30)     NOT NULL DEFAULT 'STANDARD',
    scheduled_date        DATE,
    scheduled_time        TIME,
    address               VARCHAR(500),
    special_instructions  VARCHAR(1000),
    subtotal              DECIMAL(12,2)   NOT NULL DEFAULT 0,
    discount_amount       DECIMAL(12,2)   DEFAULT 0,
    tax_amount            DECIMAL(12,2)   DEFAULT 0,
    total_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    payment_status        VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    payment_method        VARCHAR(30),
    cancelled_by          BIGINT          REFERENCES app_user(id),
    cancellation_reason   VARCHAR(500),
    completed_at          TIMESTAMP,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_booking_items (
    id                BIGSERIAL       PRIMARY KEY,
    booking_id        BIGINT          NOT NULL REFERENCES vms_bookings(id) ON DELETE CASCADE,
    service_id        BIGINT          REFERENCES vms_vendor_services(id),
    service_name      VARCHAR(200)    NOT NULL,
    quantity          INT             NOT NULL DEFAULT 1,
    unit_price        DECIMAL(12,2)   NOT NULL,
    discount          DECIMAL(12,2)   DEFAULT 0,
    tax               DECIMAL(12,2)   DEFAULT 0,
    total             DECIMAL(12,2)   NOT NULL
);

CREATE TABLE IF NOT EXISTS vms_booking_status_history (
    id                BIGSERIAL       PRIMARY KEY,
    booking_id        BIGINT          NOT NULL REFERENCES vms_bookings(id) ON DELETE CASCADE,
    from_status       VARCHAR(30),
    to_status         VARCHAR(30)     NOT NULL,
    changed_by        BIGINT          REFERENCES app_user(id),
    notes             VARCHAR(500),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_booking_reschedules (
    id                    BIGSERIAL       PRIMARY KEY,
    booking_id            BIGINT          NOT NULL REFERENCES vms_bookings(id) ON DELETE CASCADE,
    original_date         DATE            NOT NULL,
    original_time         TIME,
    new_date              DATE            NOT NULL,
    new_time              TIME,
    reason                VARCHAR(500),
    requested_by          BIGINT          NOT NULL REFERENCES app_user(id),
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_favorites (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id) ON DELETE CASCADE,
    user_id           BIGINT          NOT NULL REFERENCES app_user(id),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(vendor_id, user_id)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 7. WORK ORDERS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_work_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    work_order_number     VARCHAR(30)     NOT NULL,
    title                 VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    type                  VARCHAR(30)     NOT NULL DEFAULT 'REPAIR',
    priority              VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    status                VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    vendor_id             BIGINT          REFERENCES vms_vendors(id),
    assigned_to           BIGINT          REFERENCES app_user(id),
    booking_id            BIGINT          REFERENCES vms_bookings(id),
    ticket_id             BIGINT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    location              VARCHAR(300),
    scheduled_date        DATE,
    scheduled_time        TIME,
    started_at            TIMESTAMP,
    completed_at          TIMESTAMP,
    estimated_cost        DECIMAL(12,2),
    actual_cost           DECIMAL(12,2),
    resident_approved     BOOLEAN         DEFAULT FALSE,
    resident_approved_at  TIMESTAMP,
    created_by            BIGINT          NOT NULL REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_work_order_items (
    id                BIGSERIAL       PRIMARY KEY,
    work_order_id     BIGINT          NOT NULL REFERENCES vms_work_orders(id) ON DELETE CASCADE,
    description       VARCHAR(500)    NOT NULL,
    quantity          INT             NOT NULL DEFAULT 1,
    unit_price        DECIMAL(12,2)   DEFAULT 0,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    completed_at      TIMESTAMP,
    sort_order        INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vms_work_order_materials (
    id                BIGSERIAL       PRIMARY KEY,
    work_order_id     BIGINT          NOT NULL REFERENCES vms_work_orders(id) ON DELETE CASCADE,
    material_name     VARCHAR(200)    NOT NULL,
    quantity          DECIMAL(10,2)   NOT NULL DEFAULT 1,
    unit              VARCHAR(30),
    unit_cost         DECIMAL(12,2)   DEFAULT 0,
    total_cost        DECIMAL(12,2)   DEFAULT 0,
    provided_by       VARCHAR(30)     DEFAULT 'VENDOR'
);

CREATE TABLE IF NOT EXISTS vms_work_order_photos (
    id                BIGSERIAL       PRIMARY KEY,
    work_order_id     BIGINT          NOT NULL REFERENCES vms_work_orders(id) ON DELETE CASCADE,
    url               VARCHAR(500)    NOT NULL,
    photo_type        VARCHAR(20)     NOT NULL DEFAULT 'BEFORE',
    caption           VARCHAR(300),
    uploaded_by       BIGINT          REFERENCES app_user(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_work_order_signatures (
    id                BIGSERIAL       PRIMARY KEY,
    work_order_id     BIGINT          NOT NULL REFERENCES vms_work_orders(id) ON DELETE CASCADE,
    signer_type       VARCHAR(20)     NOT NULL,
    signer_name       VARCHAR(150)    NOT NULL,
    signature_url     VARCHAR(500)    NOT NULL,
    signed_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_work_order_status_history (
    id                BIGSERIAL       PRIMARY KEY,
    work_order_id     BIGINT          NOT NULL REFERENCES vms_work_orders(id) ON DELETE CASCADE,
    from_status       VARCHAR(30),
    to_status         VARCHAR(30)     NOT NULL,
    changed_by        BIGINT          REFERENCES app_user(id),
    notes             VARCHAR(500),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 8. PROCUREMENT
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_purchase_requests (
    id                    BIGSERIAL       PRIMARY KEY,
    request_number        VARCHAR(30)     NOT NULL,
    title                 VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    priority              VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    required_by_date      DATE,
    department            VARCHAR(100),
    total_estimated_cost  DECIMAL(12,2)   DEFAULT 0,
    approved_by           BIGINT          REFERENCES app_user(id),
    approved_at           TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_by            BIGINT          NOT NULL REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_purchase_request_items (
    id                BIGSERIAL       PRIMARY KEY,
    request_id        BIGINT          NOT NULL REFERENCES vms_purchase_requests(id) ON DELETE CASCADE,
    item_name         VARCHAR(200)    NOT NULL,
    specification     VARCHAR(500),
    quantity          DECIMAL(10,2)   NOT NULL DEFAULT 1,
    unit              VARCHAR(30),
    estimated_price   DECIMAL(12,2)   DEFAULT 0,
    sort_order        INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vms_quotations (
    id                    BIGSERIAL       PRIMARY KEY,
    quotation_number      VARCHAR(30)     NOT NULL,
    purchase_request_id   BIGINT          REFERENCES vms_purchase_requests(id),
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'SUBMITTED',
    total_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_amount            DECIMAL(12,2)   DEFAULT 0,
    discount_amount       DECIMAL(12,2)   DEFAULT 0,
    grand_total           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    delivery_days         INT,
    warranty_days         INT,
    payment_terms         VARCHAR(500),
    validity_days         INT             DEFAULT 30,
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    submitted_at          TIMESTAMP,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_quotation_items (
    id                BIGSERIAL       PRIMARY KEY,
    quotation_id      BIGINT          NOT NULL REFERENCES vms_quotations(id) ON DELETE CASCADE,
    item_name         VARCHAR(200)    NOT NULL,
    specification     VARCHAR(500),
    quantity          DECIMAL(10,2)   NOT NULL DEFAULT 1,
    unit              VARCHAR(30),
    unit_price        DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_percent       DECIMAL(5,2)    DEFAULT 0,
    discount_percent  DECIMAL(5,2)    DEFAULT 0,
    total             DECIMAL(12,2)   NOT NULL DEFAULT 0,
    sort_order        INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vms_quotation_comparisons (
    id                    BIGSERIAL       PRIMARY KEY,
    purchase_request_id   BIGINT          NOT NULL REFERENCES vms_purchase_requests(id),
    title                 VARCHAR(200),
    status                VARCHAR(30)     NOT NULL DEFAULT 'IN_PROGRESS',
    selected_quotation_id BIGINT          REFERENCES vms_quotations(id),
    selected_by           BIGINT          REFERENCES app_user(id),
    selected_at           TIMESTAMP,
    ai_recommendation     VARCHAR(2000),
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_by            BIGINT          NOT NULL REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_purchase_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    po_number             VARCHAR(30)     NOT NULL,
    quotation_id          BIGINT          REFERENCES vms_quotations(id),
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    total_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_amount            DECIMAL(12,2)   DEFAULT 0,
    discount_amount       DECIMAL(12,2)   DEFAULT 0,
    grand_total           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    delivery_date         DATE,
    delivery_address      VARCHAR(500),
    payment_terms         VARCHAR(500),
    terms_conditions      VARCHAR(2000),
    approved_by           BIGINT          REFERENCES app_user(id),
    approved_at           TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_by            BIGINT          NOT NULL REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_purchase_order_items (
    id                BIGSERIAL       PRIMARY KEY,
    po_id             BIGINT          NOT NULL REFERENCES vms_purchase_orders(id) ON DELETE CASCADE,
    item_name         VARCHAR(200)    NOT NULL,
    specification     VARCHAR(500),
    quantity          DECIMAL(10,2)   NOT NULL DEFAULT 1,
    unit              VARCHAR(30),
    unit_price        DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_percent       DECIMAL(5,2)    DEFAULT 0,
    discount_percent  DECIMAL(5,2)    DEFAULT 0,
    total             DECIMAL(12,2)   NOT NULL DEFAULT 0,
    received_quantity DECIMAL(10,2)   DEFAULT 0,
    sort_order        INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vms_goods_receipts (
    id                    BIGSERIAL       PRIMARY KEY,
    grn_number            VARCHAR(30)     NOT NULL,
    po_id                 BIGINT          NOT NULL REFERENCES vms_purchase_orders(id),
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    received_date         DATE            NOT NULL,
    received_by           BIGINT          NOT NULL REFERENCES app_user(id),
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_goods_receipt_items (
    id                BIGSERIAL       PRIMARY KEY,
    grn_id            BIGINT          NOT NULL REFERENCES vms_goods_receipts(id) ON DELETE CASCADE,
    po_item_id        BIGINT          REFERENCES vms_purchase_order_items(id),
    item_name         VARCHAR(200)    NOT NULL,
    ordered_quantity  DECIMAL(10,2)   NOT NULL,
    received_quantity DECIMAL(10,2)   NOT NULL,
    accepted_quantity DECIMAL(10,2)   NOT NULL,
    rejected_quantity DECIMAL(10,2)   DEFAULT 0,
    rejection_reason  VARCHAR(500),
    sort_order        INT             NOT NULL DEFAULT 0
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 9. CONTRACT MANAGEMENT
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_contracts (
    id                    BIGSERIAL       PRIMARY KEY,
    contract_number       VARCHAR(30)     NOT NULL,
    title                 VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    type                  VARCHAR(30)     NOT NULL DEFAULT 'SERVICE',
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    start_date            DATE            NOT NULL,
    end_date              DATE            NOT NULL,
    value                 DECIMAL(14,2)   DEFAULT 0,
    payment_frequency     VARCHAR(20),
    auto_renew            BOOLEAN         NOT NULL DEFAULT FALSE,
    renewal_notice_days   INT             DEFAULT 30,
    penalty_clause        VARCHAR(2000),
    termination_clause    VARCHAR(2000),
    signed_by_vendor      BOOLEAN         NOT NULL DEFAULT FALSE,
    signed_by_admin       BOOLEAN         NOT NULL DEFAULT FALSE,
    signed_at             TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_by            BIGINT          NOT NULL REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_contract_documents (
    id                BIGSERIAL       PRIMARY KEY,
    contract_id       BIGINT          NOT NULL REFERENCES vms_contracts(id) ON DELETE CASCADE,
    document_name     VARCHAR(200)    NOT NULL,
    file_url          VARCHAR(500)    NOT NULL,
    file_size         BIGINT,
    document_type     VARCHAR(50),
    uploaded_by       BIGINT          REFERENCES app_user(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_contract_sla (
    id                BIGSERIAL       PRIMARY KEY,
    contract_id       BIGINT          NOT NULL REFERENCES vms_contracts(id) ON DELETE CASCADE,
    metric_name       VARCHAR(150)    NOT NULL,
    target_value      VARCHAR(100)    NOT NULL,
    unit              VARCHAR(30),
    penalty_amount    DECIMAL(12,2)   DEFAULT 0,
    measurement_freq  VARCHAR(30),
    sort_order        INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vms_contract_escalation_matrix (
    id                BIGSERIAL       PRIMARY KEY,
    contract_id       BIGINT          NOT NULL REFERENCES vms_contracts(id) ON DELETE CASCADE,
    level             INT             NOT NULL,
    contact_name      VARCHAR(150)    NOT NULL,
    contact_email     VARCHAR(150),
    contact_phone     VARCHAR(20),
    designation       VARCHAR(100),
    escalation_time   INT,
    time_unit         VARCHAR(20)     DEFAULT 'HOURS'
);

CREATE TABLE IF NOT EXISTS vms_contract_versions (
    id                BIGSERIAL       PRIMARY KEY,
    contract_id       BIGINT          NOT NULL REFERENCES vms_contracts(id) ON DELETE CASCADE,
    version           INT             NOT NULL,
    change_summary    VARCHAR(500),
    file_url          VARCHAR(500),
    created_by        BIGINT          REFERENCES app_user(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 10. PAYMENTS & INVOICING
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_invoices (
    id                    BIGSERIAL       PRIMARY KEY,
    invoice_number        VARCHAR(30)     NOT NULL,
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    booking_id            BIGINT          REFERENCES vms_bookings(id),
    work_order_id         BIGINT          REFERENCES vms_work_orders(id),
    po_id                 BIGINT          REFERENCES vms_purchase_orders(id),
    contract_id           BIGINT          REFERENCES vms_contracts(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    invoice_date          DATE            NOT NULL,
    due_date              DATE,
    subtotal              DECIMAL(14,2)   NOT NULL DEFAULT 0,
    discount_amount       DECIMAL(12,2)   DEFAULT 0,
    tax_amount            DECIMAL(12,2)   DEFAULT 0,
    tds_amount            DECIMAL(12,2)   DEFAULT 0,
    total_amount          DECIMAL(14,2)   NOT NULL DEFAULT 0,
    paid_amount           DECIMAL(14,2)   DEFAULT 0,
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_by            BIGINT          REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_invoice_items (
    id                BIGSERIAL       PRIMARY KEY,
    invoice_id        BIGINT          NOT NULL REFERENCES vms_invoices(id) ON DELETE CASCADE,
    description       VARCHAR(500)    NOT NULL,
    quantity          DECIMAL(10,2)   NOT NULL DEFAULT 1,
    unit              VARCHAR(30),
    unit_price        DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_percent       DECIMAL(5,2)    DEFAULT 0,
    discount_percent  DECIMAL(5,2)    DEFAULT 0,
    total             DECIMAL(12,2)   NOT NULL DEFAULT 0,
    sort_order        INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vms_payments (
    id                    BIGSERIAL       PRIMARY KEY,
    payment_number        VARCHAR(30)     NOT NULL,
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    invoice_id            BIGINT          REFERENCES vms_invoices(id),
    booking_id            BIGINT          REFERENCES vms_bookings(id),
    type                  VARCHAR(30)     NOT NULL DEFAULT 'FULL',
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    amount                DECIMAL(14,2)   NOT NULL,
    payment_method        VARCHAR(30),
    transaction_id        VARCHAR(100),
    payment_date          DATE,
    notes                 VARCHAR(500),
    gst_amount            DECIMAL(12,2)   DEFAULT 0,
    tds_amount            DECIMAL(12,2)   DEFAULT 0,
    commission_amount     DECIMAL(12,2)   DEFAULT 0,
    net_amount            DECIMAL(14,2)   NOT NULL DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    processed_by          BIGINT          REFERENCES app_user(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_wallet (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id),
    balance           DECIMAL(14,2)   NOT NULL DEFAULT 0,
    total_earned      DECIMAL(14,2)   NOT NULL DEFAULT 0,
    total_withdrawn   DECIMAL(14,2)   NOT NULL DEFAULT 0,
    total_commission  DECIMAL(14,2)   NOT NULL DEFAULT 0,
    last_payout_at    TIMESTAMP,
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(vendor_id, community_id)
);

CREATE TABLE IF NOT EXISTS vms_wallet_transactions (
    id                BIGSERIAL       PRIMARY KEY,
    wallet_id         BIGINT          NOT NULL REFERENCES vms_vendor_wallet(id),
    type              VARCHAR(20)     NOT NULL,
    amount            DECIMAL(14,2)   NOT NULL,
    balance_after     DECIMAL(14,2)   NOT NULL,
    reference_type    VARCHAR(30),
    reference_id      BIGINT,
    description       VARCHAR(500),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_settlements (
    id                    BIGSERIAL       PRIMARY KEY,
    settlement_number     VARCHAR(30)     NOT NULL,
    vendor_id             BIGINT          NOT NULL REFERENCES vms_vendors(id),
    period_start          DATE            NOT NULL,
    period_end            DATE            NOT NULL,
    total_earnings        DECIMAL(14,2)   NOT NULL DEFAULT 0,
    total_commission      DECIMAL(14,2)   DEFAULT 0,
    total_tds             DECIMAL(14,2)   DEFAULT 0,
    net_payable           DECIMAL(14,2)   NOT NULL DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    paid_at               TIMESTAMP,
    transaction_id        VARCHAR(100),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 11. RATINGS & REVIEWS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_ratings (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id),
    booking_id        BIGINT          REFERENCES vms_bookings(id),
    work_order_id     BIGINT          REFERENCES vms_work_orders(id),
    user_id           BIGINT          NOT NULL REFERENCES app_user(id),
    overall_rating    DECIMAL(3,2)    NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_rating_criteria (
    id                BIGSERIAL       PRIMARY KEY,
    rating_id         BIGINT          NOT NULL REFERENCES vms_ratings(id) ON DELETE CASCADE,
    criteria_name     VARCHAR(50)     NOT NULL,
    score             DECIMAL(3,2)    NOT NULL
);

CREATE TABLE IF NOT EXISTS vms_reviews (
    id                BIGSERIAL       PRIMARY KEY,
    rating_id         BIGINT          NOT NULL REFERENCES vms_ratings(id) ON DELETE CASCADE,
    title             VARCHAR(200),
    comment           VARCHAR(2000)   NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    moderated_by      BIGINT          REFERENCES app_user(id),
    moderated_at      TIMESTAMP,
    ai_summary        VARCHAR(500),
    helpful_count     INT             DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_review_media (
    id                BIGSERIAL       PRIMARY KEY,
    review_id         BIGINT          NOT NULL REFERENCES vms_reviews(id) ON DELETE CASCADE,
    url               VARCHAR(500)    NOT NULL,
    media_type        VARCHAR(20)     NOT NULL DEFAULT 'IMAGE',
    sort_order        INT             NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 12. PERFORMANCE & ANALYTICS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_vendor_performance (
    id                        BIGSERIAL       PRIMARY KEY,
    vendor_id                 BIGINT          NOT NULL REFERENCES vms_vendors(id),
    period_type               VARCHAR(20)     NOT NULL DEFAULT 'MONTHLY',
    period_start              DATE            NOT NULL,
    period_end                DATE            NOT NULL,
    jobs_completed            INT             DEFAULT 0,
    jobs_cancelled            INT             DEFAULT 0,
    average_rating            DECIMAL(3,2)    DEFAULT 0,
    average_response_minutes  INT             DEFAULT 0,
    average_completion_minutes INT            DEFAULT 0,
    sla_compliance_percent    DECIMAL(5,2)    DEFAULT 0,
    revenue                   DECIMAL(14,2)   DEFAULT 0,
    repeat_customer_percent   DECIMAL(5,2)    DEFAULT 0,
    complaint_count           INT             DEFAULT 0,
    customer_satisfaction     DECIMAL(5,2)    DEFAULT 0,
    community_id              BIGINT          NOT NULL REFERENCES community(id),
    created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_vendor_performance_snapshots (
    id                BIGSERIAL       PRIMARY KEY,
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id),
    snapshot_date     DATE            NOT NULL,
    metric_name       VARCHAR(50)     NOT NULL,
    metric_value      DECIMAL(14,2)   NOT NULL,
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 13. NOTIFICATIONS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_notification_templates (
    id                BIGSERIAL       PRIMARY KEY,
    event_type        VARCHAR(60)     NOT NULL,
    channel           VARCHAR(20)     NOT NULL,
    subject           VARCHAR(200),
    body_template     TEXT            NOT NULL,
    active            BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_notification_logs (
    id                BIGSERIAL       PRIMARY KEY,
    template_id       BIGINT          REFERENCES vms_notification_templates(id),
    recipient_id      BIGINT          REFERENCES app_user(id),
    channel           VARCHAR(20)     NOT NULL,
    subject           VARCHAR(200),
    body              TEXT,
    status            VARCHAR(20)     NOT NULL DEFAULT 'SENT',
    reference_type    VARCHAR(30),
    reference_id      BIGINT,
    error_message     VARCHAR(500),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    sent_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_notification_preferences (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL REFERENCES app_user(id),
    event_type        VARCHAR(60)     NOT NULL,
    email_enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    sms_enabled       BOOLEAN         NOT NULL DEFAULT TRUE,
    push_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    whatsapp_enabled  BOOLEAN         NOT NULL DEFAULT FALSE,
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    UNIQUE(user_id, event_type, community_id)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 14. AI FEATURES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_ai_recommendations (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL REFERENCES app_user(id),
    vendor_id         BIGINT          NOT NULL REFERENCES vms_vendors(id),
    service_id        BIGINT          REFERENCES vms_vendor_services(id),
    score             DECIMAL(5,4)    NOT NULL,
    reason            VARCHAR(500),
    context_type      VARCHAR(30),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vms_ai_predictions (
    id                BIGSERIAL       PRIMARY KEY,
    prediction_type   VARCHAR(50)     NOT NULL,
    entity_type       VARCHAR(30)     NOT NULL,
    entity_id         BIGINT          NOT NULL,
    predicted_value   VARCHAR(200)    NOT NULL,
    confidence        DECIMAL(5,4),
    actual_value      VARCHAR(200),
    model_version     VARCHAR(30),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 15. AUDIT
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vms_audit_log (
    id                BIGSERIAL       PRIMARY KEY,
    entity_type       VARCHAR(50)     NOT NULL,
    entity_id         BIGINT          NOT NULL,
    action            VARCHAR(30)     NOT NULL,
    field_name        VARCHAR(100),
    old_value         TEXT,
    new_value         TEXT,
    performed_by      BIGINT          REFERENCES app_user(id),
    ip_address        VARCHAR(50),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ═══════════════════════════════════════════════════════════════════════════
-- INDEXES
-- ═══════════════════════════════════════════════════════════════════════════

-- Vendor Categories
CREATE INDEX IF NOT EXISTS idx_vms_vendor_categories_community ON vms_vendor_categories(community_id);
CREATE INDEX IF NOT EXISTS idx_vms_vendor_categories_parent ON vms_vendor_categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_vms_vendor_categories_slug ON vms_vendor_categories(slug, community_id);

-- Vendors
CREATE INDEX IF NOT EXISTS idx_vms_vendors_community_status ON vms_vendors(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_vendors_category ON vms_vendors(category_id);
CREATE INDEX IF NOT EXISTS idx_vms_vendors_user ON vms_vendors(user_id);
CREATE INDEX IF NOT EXISTS idx_vms_vendors_slug ON vms_vendors(slug);
CREATE INDEX IF NOT EXISTS idx_vms_vendors_rating ON vms_vendors(average_rating DESC);

-- Vendor Services
CREATE INDEX IF NOT EXISTS idx_vms_vendor_services_vendor ON vms_vendor_services(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_vendor_services_category ON vms_vendor_services(category_id);

-- Bookings
CREATE INDEX IF NOT EXISTS idx_vms_bookings_vendor ON vms_bookings(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_bookings_resident ON vms_bookings(resident_id);
CREATE INDEX IF NOT EXISTS idx_vms_bookings_community ON vms_bookings(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_bookings_date ON vms_bookings(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_vms_bookings_number ON vms_bookings(booking_number);

-- Work Orders
CREATE INDEX IF NOT EXISTS idx_vms_work_orders_vendor ON vms_work_orders(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_work_orders_community ON vms_work_orders(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_work_orders_number ON vms_work_orders(work_order_number);
CREATE INDEX IF NOT EXISTS idx_vms_work_orders_date ON vms_work_orders(scheduled_date);

-- Purchase Orders
CREATE INDEX IF NOT EXISTS idx_vms_purchase_orders_vendor ON vms_purchase_orders(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_purchase_orders_community ON vms_purchase_orders(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_purchase_orders_number ON vms_purchase_orders(po_number);

-- Quotations
CREATE INDEX IF NOT EXISTS idx_vms_quotations_vendor ON vms_quotations(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vms_quotations_pr ON vms_quotations(purchase_request_id);

-- Contracts
CREATE INDEX IF NOT EXISTS idx_vms_contracts_vendor ON vms_contracts(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_contracts_community ON vms_contracts(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_contracts_end_date ON vms_contracts(end_date);
CREATE INDEX IF NOT EXISTS idx_vms_contracts_number ON vms_contracts(contract_number);

-- Invoices
CREATE INDEX IF NOT EXISTS idx_vms_invoices_vendor ON vms_invoices(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_invoices_community ON vms_invoices(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_invoices_number ON vms_invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_vms_invoices_due_date ON vms_invoices(due_date);

-- Payments
CREATE INDEX IF NOT EXISTS idx_vms_payments_vendor ON vms_payments(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_payments_community ON vms_payments(community_id);
CREATE INDEX IF NOT EXISTS idx_vms_payments_number ON vms_payments(payment_number);

-- Ratings
CREATE INDEX IF NOT EXISTS idx_vms_ratings_vendor ON vms_ratings(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vms_ratings_user ON vms_ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_vms_ratings_booking ON vms_ratings(booking_id);

-- Performance
CREATE INDEX IF NOT EXISTS idx_vms_vendor_performance_vendor ON vms_vendor_performance(vendor_id, period_type);
CREATE INDEX IF NOT EXISTS idx_vms_vendor_performance_period ON vms_vendor_performance(period_start, period_end);

-- Documents
CREATE INDEX IF NOT EXISTS idx_vms_vendor_documents_vendor ON vms_vendor_documents(vendor_id, document_type);
CREATE INDEX IF NOT EXISTS idx_vms_vendor_documents_expiry ON vms_vendor_documents(expiry_date);

-- Favorites
CREATE INDEX IF NOT EXISTS idx_vms_vendor_favorites_user ON vms_vendor_favorites(user_id, community_id);

-- Notifications
CREATE INDEX IF NOT EXISTS idx_vms_notification_logs_recipient ON vms_notification_logs(recipient_id, community_id);
CREATE INDEX IF NOT EXISTS idx_vms_notification_logs_sent ON vms_notification_logs(sent_at DESC);

-- AI
CREATE INDEX IF NOT EXISTS idx_vms_ai_recommendations_user ON vms_ai_recommendations(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_vms_ai_predictions_entity ON vms_ai_predictions(entity_type, entity_id);

-- Audit
CREATE INDEX IF NOT EXISTS idx_vms_audit_log_entity ON vms_audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_vms_audit_log_community ON vms_audit_log(community_id, created_at DESC);

-- Registrations
CREATE INDEX IF NOT EXISTS idx_vms_vendor_registrations_community ON vms_vendor_registrations(community_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_vendor_registrations_email ON vms_vendor_registrations(email);

-- Invitations
CREATE INDEX IF NOT EXISTS idx_vms_vendor_invitations_code ON vms_vendor_invitations(invitation_code);

-- Settlements
CREATE INDEX IF NOT EXISTS idx_vms_settlements_vendor ON vms_settlements(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vms_settlements_number ON vms_settlements(settlement_number);

-- Wallet
CREATE INDEX IF NOT EXISTS idx_vms_wallet_transactions_wallet ON vms_wallet_transactions(wallet_id, created_at DESC);

-- Working Hours
CREATE INDEX IF NOT EXISTS idx_vms_vendor_working_hours_vendor ON vms_vendor_working_hours(vendor_id, day_of_week);

-- Service Areas
CREATE INDEX IF NOT EXISTS idx_vms_vendor_service_areas_vendor ON vms_vendor_service_areas(vendor_id);
