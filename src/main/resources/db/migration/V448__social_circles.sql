-- Social groups: resident-created interest groups within a community
CREATE TABLE social_groups (
  id               BIGSERIAL PRIMARY KEY,
  community_id     BIGINT NOT NULL,
  created_by       BIGINT NOT NULL,
  name             VARCHAR(200) NOT NULL,
  description      TEXT,
  group_type       VARCHAR(50) NOT NULL DEFAULT 'INTEREST',
  cover_image      VARCHAR(500),
  is_private       BOOLEAN NOT NULL DEFAULT FALSE,
  max_members      INT DEFAULT 50,
  member_count     INT NOT NULL DEFAULT 1,
  is_active        BOOLEAN NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE social_group_members (
  id               BIGSERIAL PRIMARY KEY,
  group_id         BIGINT NOT NULL REFERENCES social_groups(id),
  user_id          BIGINT NOT NULL,
  role             VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
  joined_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(group_id, user_id)
);

CREATE TABLE group_messages (
  id               BIGSERIAL PRIMARY KEY,
  group_id         BIGINT NOT NULL REFERENCES social_groups(id),
  sender_id        BIGINT NOT NULL,
  content          TEXT NOT NULL,
  message_type     VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  media_url        VARCHAR(500),
  sent_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  edited_at        TIMESTAMPTZ,
  deleted_at       TIMESTAMPTZ
);

CREATE TABLE group_events (
  id               BIGSERIAL PRIMARY KEY,
  group_id         BIGINT NOT NULL REFERENCES social_groups(id),
  created_by       BIGINT NOT NULL,
  title            VARCHAR(300) NOT NULL,
  description      TEXT,
  event_date       TIMESTAMPTZ NOT NULL,
  location         VARCHAR(300),
  rsvp_deadline    TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE group_event_rsvps (
  id               BIGSERIAL PRIMARY KEY,
  event_id         BIGINT NOT NULL REFERENCES group_events(id),
  user_id          BIGINT NOT NULL,
  response         VARCHAR(20) NOT NULL,
  responded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(event_id, user_id)
);

CREATE TABLE group_expenses (
  id               BIGSERIAL PRIMARY KEY,
  group_id         BIGINT NOT NULL REFERENCES social_groups(id),
  paid_by          BIGINT NOT NULL,
  amount           NUMERIC(10,2) NOT NULL,
  description      VARCHAR(300) NOT NULL,
  split_type       VARCHAR(20) NOT NULL DEFAULT 'EQUAL',
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE group_expense_splits (
  id               BIGSERIAL PRIMARY KEY,
  expense_id       BIGINT NOT NULL REFERENCES group_expenses(id),
  member_id        BIGINT NOT NULL,
  amount_owed      NUMERIC(10,2) NOT NULL,
  is_settled       BOOLEAN NOT NULL DEFAULT FALSE,
  settled_at       TIMESTAMPTZ
);

CREATE INDEX idx_social_groups_community ON social_groups(community_id, is_active);
CREATE INDEX idx_group_members_user ON social_group_members(user_id);
CREATE INDEX idx_group_messages_group ON group_messages(group_id, sent_at DESC);
CREATE INDEX idx_group_events_group ON group_events(group_id, event_date);
CREATE INDEX idx_group_expense_splits_member ON group_expense_splits(member_id, is_settled);
