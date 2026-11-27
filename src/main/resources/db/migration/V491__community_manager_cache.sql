-- Community Manager Dashboard snapshot cache
CREATE TABLE community_manager_snapshots (
    id                    BIGSERIAL PRIMARY KEY,
    community_id          BIGINT       NOT NULL,
    snapshot_json         TEXT         NOT NULL,
    captured_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_community_manager_snapshot_community
    ON community_manager_snapshots (community_id);
