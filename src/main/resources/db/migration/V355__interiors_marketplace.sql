ALTER TABLE service_provider
    ADD COLUMN IF NOT EXISTS portfolio jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE sp_service_quote
    ADD COLUMN IF NOT EXISTS estimated_duration_days integer,
    ADD COLUMN IF NOT EXISTS material_allowance numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS labor_amount numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_amount numeric(12, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS sp_service_quote_line_item (
    id bigserial PRIMARY KEY,
    service_quote_id bigint NOT NULL REFERENCES sp_service_quote(id) ON DELETE CASCADE,
    category varchar(40) NOT NULL,
    description varchar(240) NOT NULL,
    quantity numeric(12, 3) NOT NULL,
    unit varchar(24) NOT NULL,
    unit_price numeric(12, 2) NOT NULL,
    margin_percent numeric(6, 2) NOT NULL DEFAULT 0,
    tax_percent numeric(6, 2) NOT NULL DEFAULT 0,
    line_total numeric(12, 2) NOT NULL,
    position integer NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_interior_quote_line_quote
    ON sp_service_quote_line_item(service_quote_id, position);

CREATE TABLE IF NOT EXISTS interior_project_invitation (
    id bigserial PRIMARY KEY,
    service_request_id bigint NOT NULL REFERENCES service_request(id) ON DELETE CASCADE,
    provider_id bigint NOT NULL REFERENCES service_provider(id) ON DELETE CASCADE,
    status varchar(20) NOT NULL DEFAULT 'INVITED',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_interior_invitation_project_provider UNIQUE (service_request_id, provider_id)
);

CREATE INDEX IF NOT EXISTS idx_interior_invitation_provider_status
    ON interior_project_invitation(provider_id, status);
CREATE INDEX IF NOT EXISTS idx_interior_invitation_request
    ON interior_project_invitation(service_request_id);

INSERT INTO service_category (domain_id, name, slug, icon, description, display_order, active, created_at, updated_at)
SELECT d.id, 'Interior Works', 'interior-works', '🏗', 'Interior design, furnishing, and home makeover projects', 16, true, now(), now()
FROM service_domain d
WHERE d.slug = 'home-services'
  AND NOT EXISTS (SELECT 1 FROM service_category c WHERE c.domain_id = d.id AND c.slug = 'interior-works');

INSERT INTO service_category (domain_id, name, slug, icon, description, display_order, active, created_at, updated_at)
SELECT d.id, 'Renovation', 'renovation', '🏘', 'Residential renovation and remodelling projects', 17, true, now(), now()
FROM service_domain d
WHERE d.slug = 'home-services'
  AND NOT EXISTS (SELECT 1 FROM service_category c WHERE c.domain_id = d.id AND c.slug = 'renovation');

CREATE TABLE IF NOT EXISTS interior_saved_designer (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider_id bigint NOT NULL REFERENCES service_provider(id) ON DELETE CASCADE,
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_interior_saved_designer UNIQUE (user_id, provider_id)
);
CREATE INDEX IF NOT EXISTS idx_interior_saved_designer_user
    ON interior_saved_designer(user_id, created_at DESC);
