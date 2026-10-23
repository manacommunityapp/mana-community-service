-- Group Buying & Deals module (our service layer tables)

CREATE TABLE IF NOT EXISTS manacommunity.group_deal (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    created_by_id    BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    title            VARCHAR(200)  NOT NULL,
    description      TEXT,
    category         VARCHAR(80)   NOT NULL,
    sub_category     VARCHAR(80),
    image_url        TEXT,
    mrp              NUMERIC(12,2),
    vendor_name      VARCHAR(150),
    vendor_id        BIGINT        REFERENCES manacommunity.app_user(id),
    vendor_rating    NUMERIC(3,1),
    vendor_verified  BOOLEAN       NOT NULL DEFAULT FALSE,
    target_participants  INT       NOT NULL DEFAULT 10,
    current_participants INT       NOT NULL DEFAULT 0,
    end_date         TIMESTAMP     NOT NULL,
    delivery_date    TIMESTAMP,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    is_festival      BOOLEAN       NOT NULL DEFAULT FALSE,
    festival_name    VARCHAR(100),
    tags             TEXT,
    pickup_points    TEXT,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.group_deal_tier (
    id          BIGSERIAL PRIMARY KEY,
    deal_id     BIGINT        NOT NULL REFERENCES manacommunity.group_deal(id) ON DELETE CASCADE,
    min_qty     INT           NOT NULL,
    max_qty     INT,
    price_per_unit NUMERIC(12,2) NOT NULL,
    label       VARCHAR(80),
    sort_order  INT           NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS manacommunity.group_order (
    id               BIGSERIAL PRIMARY KEY,
    deal_id          BIGINT        NOT NULL REFERENCES manacommunity.group_deal(id),
    user_id          BIGINT        NOT NULL REFERENCES manacommunity.app_user(id),
    community_id     BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    order_number     VARCHAR(40)   NOT NULL UNIQUE,
    quantity         INT           NOT NULL DEFAULT 1,
    price_per_unit   NUMERIC(12,2) NOT NULL,
    total_amount     NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED',
    fulfillment      VARCHAR(20)   NOT NULL DEFAULT 'PICKUP',
    payment_method   VARCHAR(20)   NOT NULL DEFAULT 'UPI',
    pickup_point     VARCHAR(200),
    delivery_address TEXT,
    qr_code          VARCHAR(100),
    reviewed         BOOLEAN       NOT NULL DEFAULT FALSE,
    ordered_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.group_demand_item (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL REFERENCES manacommunity.community(id),
    requested_by_id BIGINT       NOT NULL REFERENCES manacommunity.app_user(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    category        VARCHAR(80)  NOT NULL,
    upvotes         INT          NOT NULL DEFAULT 1,
    target_upvotes  INT          NOT NULL DEFAULT 20,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.group_demand_upvote (
    demand_id BIGINT NOT NULL REFERENCES manacommunity.group_demand_item(id) ON DELETE CASCADE,
    user_id   BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    PRIMARY KEY (demand_id, user_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_group_deal_community    ON manacommunity.group_deal(community_id);
CREATE INDEX IF NOT EXISTS idx_group_deal_status       ON manacommunity.group_deal(status);
CREATE INDEX IF NOT EXISTS idx_group_order_user        ON manacommunity.group_order(user_id);
CREATE INDEX IF NOT EXISTS idx_group_order_deal        ON manacommunity.group_order(deal_id);
CREATE INDEX IF NOT EXISTS idx_group_demand_community  ON manacommunity.group_demand_item(community_id);
