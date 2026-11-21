-- Cross-community federation: Mana-enabled communities can federate to share verified identity
CREATE TABLE community_federation_links (
  id               BIGSERIAL PRIMARY KEY,
  community_id_a   BIGINT NOT NULL,
  community_id_b   BIGINT NOT NULL,
  link_type        VARCHAR(30) NOT NULL DEFAULT 'IDENTITY_SHARE',
                   -- IDENTITY_SHARE, SPORTS_LEAGUE, VENDOR_RATINGS, FULL
  status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                   -- PENDING, ACTIVE, SUSPENDED
  initiated_by     BIGINT NOT NULL,
  approved_by      BIGINT,
  approved_at      TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(community_id_a, community_id_b, link_type)
);

-- Resident profile export bundle for portability (token shared only with the resident)
CREATE TABLE resident_portability_tokens (
  id                   BIGSERIAL PRIMARY KEY,
  resident_id          BIGINT NOT NULL,
  source_community_id  BIGINT NOT NULL,
  token_hash           VARCHAR(100) NOT NULL UNIQUE, -- SHA-256, shared with resident only
  export_payload       JSONB NOT NULL,
  -- payload: {name, verified, kyc_method, reputation_score, tenure_months,
  --           amenity_usage, payment_history_summary, activity_badges}
  expires_at           TIMESTAMPTZ NOT NULL,         -- 30-day window to claim
  claimed_at           TIMESTAMPTZ,
  claimed_by_community BIGINT,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Resident reputation: computed from participation, payments, community role
CREATE TABLE resident_reputation_scores (
  id               BIGSERIAL PRIMARY KEY,
  resident_id      BIGINT NOT NULL,
  community_id     BIGINT NOT NULL,
  score            NUMERIC(5,2) NOT NULL DEFAULT 50.00, -- 0–100
  payment_score    NUMERIC(5,2),   -- maintenance payment reliability
  participation    NUMERIC(5,2),   -- events, votes, academy, groups
  tenure_months    INT NOT NULL DEFAULT 0,
  badges           JSONB,          -- [{type, label, earned_at}]
  computed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(resident_id, community_id)
);

CREATE INDEX idx_portability_resident ON resident_portability_tokens(resident_id, expires_at);
CREATE INDEX idx_reputation_community ON resident_reputation_scores(community_id, score DESC);
CREATE INDEX idx_federation_community ON community_federation_links(community_id_a, status);
CREATE INDEX idx_federation_community_b ON community_federation_links(community_id_b, status);
