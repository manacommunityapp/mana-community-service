-- Committee elections: positions, nominations, and votes
CREATE TABLE committee_positions (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    max_holders     SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_committee_positions_community ON committee_positions(community_id);

CREATE TABLE committee_elections (
    id               BIGSERIAL   PRIMARY KEY,
    community_id     BIGINT      NOT NULL,
    position_id      BIGINT      NOT NULL REFERENCES committee_positions(id),
    nomination_start TIMESTAMPTZ NOT NULL,
    nomination_end   TIMESTAMPTZ NOT NULL,
    voting_start     TIMESTAMPTZ NOT NULL,
    voting_end       TIMESTAMPTZ NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'NOMINATIONS_OPEN',
    -- NOMINATIONS_OPEN, VOTING_OPEN, CLOSED, CANCELLED
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_committee_elections_community ON committee_elections(community_id, status);

CREATE TABLE election_nominations (
    id            BIGSERIAL   PRIMARY KEY,
    election_id   BIGINT      NOT NULL REFERENCES committee_elections(id),
    nominee_id    BIGINT      NOT NULL,
    manifesto     TEXT,
    nominated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (election_id, nominee_id)
);
CREATE INDEX idx_election_nominations_election ON election_nominations(election_id);

CREATE TABLE election_votes (
    id            BIGSERIAL   PRIMARY KEY,
    election_id   BIGINT      NOT NULL REFERENCES committee_elections(id),
    voter_id      BIGINT      NOT NULL,
    nomination_id BIGINT      NOT NULL REFERENCES election_nominations(id),
    voted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (election_id, voter_id)   -- one vote per resident per election
);
CREATE INDEX idx_election_votes_election ON election_votes(election_id);
