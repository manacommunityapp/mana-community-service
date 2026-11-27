-- Resident-initiated pooled buying: residents coordinate demand, single delivery to gate
CREATE TABLE IF NOT EXISTS group_orders (
  id               BIGSERIAL PRIMARY KEY,
  community_id     BIGINT NOT NULL,
  organizer_id     BIGINT NOT NULL,
  title            VARCHAR(300) NOT NULL,
  description      TEXT,
  category         VARCHAR(50) NOT NULL,
  source_vendor    VARCHAR(200),
  item_name        VARCHAR(300) NOT NULL,
  unit_price       NUMERIC(10,2),
  unit_description VARCHAR(100),
  min_quantity     INT NOT NULL DEFAULT 5,
  max_quantity     INT,
  total_committed  INT NOT NULL DEFAULT 0,
  closes_at        TIMESTAMPTZ NOT NULL,
  delivery_date    DATE,
  delivery_point   VARCHAR(200),
  status           VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  final_unit_price NUMERIC(10,2),
  vendor_order_ref VARCHAR(200),
  image_url        VARCHAR(500),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS group_order_contributions (
  id                  BIGSERIAL PRIMARY KEY,
  group_order_id      BIGINT NOT NULL REFERENCES group_orders(id),
  resident_id         BIGINT NOT NULL,
  quantity            INT NOT NULL DEFAULT 1,
  unit_price_at_join  NUMERIC(10,2),
  total_amount        NUMERIC(10,2),
  payment_status      VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  payment_ref         VARCHAR(200),
  collection_status   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  joined_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(group_order_id, resident_id)
);

CREATE TABLE IF NOT EXISTS group_order_updates (
  id              BIGSERIAL PRIMARY KEY,
  group_order_id  BIGINT NOT NULL REFERENCES group_orders(id),
  update_type     VARCHAR(50) NOT NULL,
  message         TEXT NOT NULL,
  posted_by       BIGINT NOT NULL,
  posted_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_group_orders_community
  ON group_orders(community_id, status, closes_at);
CREATE INDEX IF NOT EXISTS idx_group_order_contributions
  ON group_order_contributions(group_order_id, resident_id);
CREATE INDEX IF NOT EXISTS idx_my_contributions
  ON group_order_contributions(resident_id, payment_status);
