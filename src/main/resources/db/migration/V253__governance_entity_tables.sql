-- GovernanceMeeting, GovernanceBallot, GovernanceProposal, GovernanceResolution entities
-- These are distinct from community_meeting/community_ballot/etc. (V232) which serve the CommunityMeeting model package

CREATE TABLE governance_meeting (
    id                BIGSERIAL    PRIMARY KEY,
    community_id      BIGINT       NOT NULL,
    type              VARCHAR(10)  NOT NULL,
    title             VARCHAR(300) NOT NULL,
    description       TEXT,
    meeting_date      DATE         NOT NULL,
    meeting_time      TIME,
    venue             VARCHAR(300),
    online_link       VARCHAR(300),
    status            VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
    quorum_required   INT,
    quorum_confirmed  INT          NOT NULL DEFAULT 0,
    created_by        BIGINT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_governance_meeting_community ON governance_meeting(community_id);
CREATE INDEX idx_governance_meeting_date      ON governance_meeting(meeting_date);

CREATE TABLE governance_ballot (
    id              BIGSERIAL    PRIMARY KEY,
    community_id    BIGINT       NOT NULL,
    meeting_id      BIGINT       REFERENCES governance_meeting(id),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    options_json    TEXT,
    votes_cast      INT          NOT NULL DEFAULT 0,
    total_eligible  INT,
    close_at        TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_governance_ballot_community ON governance_ballot(community_id);
CREATE INDEX idx_governance_ballot_meeting   ON governance_ballot(meeting_id);

CREATE TABLE governance_proposal (
    id            BIGSERIAL    PRIMARY KEY,
    community_id  BIGINT       NOT NULL,
    submitted_by  BIGINT       NOT NULL,
    meeting_id    BIGINT       REFERENCES governance_meeting(id),
    title         VARCHAR(300) NOT NULL,
    description   TEXT,
    category      VARCHAR(50)  NOT NULL,
    status        VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    support_count INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_governance_proposal_community   ON governance_proposal(community_id);
CREATE INDEX idx_governance_proposal_submitted_by ON governance_proposal(submitted_by);

CREATE TABLE governance_resolution (
    id                BIGSERIAL    PRIMARY KEY,
    community_id      BIGINT       NOT NULL,
    meeting_id        BIGINT       REFERENCES governance_meeting(id),
    resolution_number VARCHAR(40)  NOT NULL,
    title             VARCHAR(300) NOT NULL,
    summary           TEXT,
    passed_on         DATE,
    status            VARCHAR(30)  NOT NULL DEFAULT 'PASSED',
    document_url      VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_governance_resolution_community ON governance_resolution(community_id);
CREATE INDEX idx_governance_resolution_number    ON governance_resolution(resolution_number);
