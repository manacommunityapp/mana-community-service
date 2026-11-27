-- Community Polls, Secret Ballot Elections & Hybrid AGM Quorum Governance

ALTER TABLE community_flat
    ADD COLUMN IF NOT EXISTS uds_sqft NUMERIC(10, 2) NOT NULL DEFAULT 1000.00,
    ADD COLUMN IF NOT EXISTS share_units INT NOT NULL DEFAULT 1;

ALTER TABLE poll
    ADD COLUMN IF NOT EXISTS voting_mode VARCHAR(32) NOT NULL DEFAULT 'ONE_FLAT_ONE_VOTE',
    ADD COLUMN IF NOT EXISTS exclude_defaulters_over_days INT NOT NULL DEFAULT 90,
    ADD COLUMN IF NOT EXISTS owners_only BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE poll_option_vote
    ADD COLUMN IF NOT EXISTS flat_id BIGINT,
    ADD COLUMN IF NOT EXISTS weight_applied NUMERIC(12, 2) NOT NULL DEFAULT 1.00,
    ADD COLUMN IF NOT EXISTS eligibility_snapshot VARCHAR(500);

ALTER TABLE committee_elections
    ADD COLUMN IF NOT EXISTS rsa_public_modulus TEXT,
    ADD COLUMN IF NOT EXISTS rsa_public_exponent VARCHAR(32),
    ADD COLUMN IF NOT EXISTS rsa_private_exponent TEXT,
    ADD COLUMN IF NOT EXISTS ballot_chain_head VARCHAR(64);

CREATE TABLE IF NOT EXISTS election_voter_roll (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    flat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    blinded_token_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_election_voter_roll_flat UNIQUE (election_id, flat_id)
);

CREATE TABLE IF NOT EXISTS election_secret_ballots (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    nomination_id BIGINT NOT NULL,
    ballot_nullifier VARCHAR(64) NOT NULL UNIQUE,
    blind_signature TEXT NOT NULL,
    prev_ballot_hash VARCHAR(64) NOT NULL,
    ballot_hash VARCHAR(64) NOT NULL,
    receipt_code VARCHAR(32) NOT NULL UNIQUE,
    cast_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_secret_ballot_election
    ON election_secret_ballots (election_id, id);

ALTER TABLE governance_meeting
    ADD COLUMN IF NOT EXISTS total_eligible_units INT NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS required_quorum_pct NUMERIC(5, 2) NOT NULL DEFAULT 25.00,
    ADD COLUMN IF NOT EXISTS quorum_met BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS max_proxies_per_holder INT NOT NULL DEFAULT 5;

ALTER TABLE governance_resolution
    ADD COLUMN IF NOT EXISTS voting_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS required_quorum_pct NUMERIC(5, 2) NOT NULL DEFAULT 25.00;

CREATE TABLE IF NOT EXISTS agm_attendance_ledger (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    flat_id BIGINT NOT NULL,
    attendee_user_id BIGINT NOT NULL,
    proxy_holder_user_id BIGINT,
    attendee_role VARCHAR(24) NOT NULL DEFAULT 'OWNER',
    attendance_mode VARCHAR(24) NOT NULL DEFAULT 'IN_PERSON_QR',
    status VARCHAR(24) NOT NULL DEFAULT 'CHECKED_IN',
    weight_uds NUMERIC(10, 2) NOT NULL DEFAULT 1.00,
    checked_in_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_agm_attendance_flat UNIQUE (meeting_id, flat_id)
);

CREATE INDEX IF NOT EXISTS idx_agm_attendance_meeting_status
    ON agm_attendance_ledger (meeting_id, status);
