-- Phase 3: Commerce Customer Profile & Addresses

CREATE TABLE IF NOT EXISTS commerce_customer_profile (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    community_id          BIGINT NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    display_name          VARCHAR(150),
    phone_number          VARCHAR(20),
    customer_status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                              CHECK (customer_status IN ('ACTIVE','INACTIVE','BLACKLISTED')),
    loyalty_points        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_orders          INTEGER      NOT NULL DEFAULT 0,
    total_spend           NUMERIC(19,4) NOT NULL DEFAULT 0,
    is_member             BOOLEAN      NOT NULL DEFAULT FALSE,
    preferred_categories  TEXT,
    kyc_verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    kyc_verified_at       TIMESTAMP,
    default_address_id    BIGINT,                           -- FK added after address table
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, community_id)
);

CREATE INDEX IF NOT EXISTS idx_ccp_user        ON commerce_customer_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_ccp_community   ON commerce_customer_profile(community_id);
CREATE INDEX IF NOT EXISTS idx_ccp_status      ON commerce_customer_profile(customer_status)
    WHERE customer_status != 'ACTIVE';

-- ── Customer delivery addresses ─────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS commerce_customer_address (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT      NOT NULL REFERENCES commerce_customer_profile(id) ON DELETE CASCADE,
    label        VARCHAR(20) NOT NULL DEFAULT 'HOME'
                     CHECK (label IN ('HOME','WORK','OTHER')),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    landmark      VARCHAR(150),
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    pincode       VARCHAR(12)  NOT NULL,
    latitude      NUMERIC(10,7),
    longitude     NUMERIC(10,7),
    is_default    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cca_customer ON commerce_customer_address(customer_id);
CREATE INDEX IF NOT EXISTS idx_cca_default  ON commerce_customer_address(customer_id, is_default)
    WHERE is_default = TRUE;

-- wire up the FK now that address table exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ccp_default_address') THEN
        ALTER TABLE commerce_customer_profile
            ADD CONSTRAINT fk_ccp_default_address
            FOREIGN KEY (default_address_id) REFERENCES commerce_customer_address(id)
            ON DELETE SET NULL;
    END IF;
END $$;
