-- Governance module: meetings, proposals, ballots, resolutions, vault

CREATE TABLE community_meeting (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT       NOT NULL REFERENCES community(id),
    type                VARCHAR(20)  NOT NULL,
    title               TEXT         NOT NULL,
    description         TEXT,
    meeting_date        DATE         NOT NULL,
    meeting_time        TIME,
    location            TEXT,
    meeting_link        TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    quorum_required     INT          DEFAULT 50,
    annual_report_url   TEXT,
    minutes_url         TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);
CREATE INDEX idx_community_meeting_community ON community_meeting(community_id);

CREATE TABLE meeting_agenda_item (
    id              BIGSERIAL PRIMARY KEY,
    meeting_id      BIGINT       NOT NULL REFERENCES community_meeting(id) ON DELETE CASCADE,
    item_order      INT          DEFAULT 1,
    title           TEXT         NOT NULL,
    presenter       TEXT,
    duration        TEXT,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    attachment_name TEXT
);
CREATE INDEX idx_meeting_agenda_item_meeting ON meeting_agenda_item(meeting_id);

CREATE TABLE meeting_rsvp (
    id              BIGSERIAL PRIMARY KEY,
    meeting_id      BIGINT      NOT NULL REFERENCES community_meeting(id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL,
    mode            VARCHAR(20) NOT NULL DEFAULT 'PHYSICAL',
    proxy_nominee   TEXT,
    confirmed_at    TIMESTAMP   DEFAULT NOW(),
    UNIQUE (meeting_id, user_id)
);
CREATE INDEX idx_meeting_rsvp_meeting ON meeting_rsvp(meeting_id);

CREATE TABLE community_proposal (
    id                    BIGSERIAL PRIMARY KEY,
    community_id          BIGINT       NOT NULL REFERENCES community(id),
    submitted_by_user_id  BIGINT       NOT NULL,
    title                 TEXT         NOT NULL,
    description           TEXT,
    category              VARCHAR(30)  NOT NULL DEFAULT 'AMENITIES',
    type                  VARCHAR(30)  NOT NULL DEFAULT 'PROPOSAL',
    estimated_cost        TEXT,
    support_threshold     INT          DEFAULT 50,
    status                VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED',
    committee_notes       TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT
);
CREATE INDEX idx_community_proposal_community ON community_proposal(community_id);

CREATE TABLE community_proposal_pros (
    proposal_id BIGINT NOT NULL REFERENCES community_proposal(id) ON DELETE CASCADE,
    pro_text    TEXT
);
CREATE INDEX idx_proposal_pros_proposal ON community_proposal_pros(proposal_id);

CREATE TABLE community_proposal_cons (
    proposal_id BIGINT NOT NULL REFERENCES community_proposal(id) ON DELETE CASCADE,
    con_text    TEXT
);
CREATE INDEX idx_proposal_cons_proposal ON community_proposal_cons(proposal_id);

CREATE TABLE proposal_support (
    id           BIGSERIAL PRIMARY KEY,
    proposal_id  BIGINT    NOT NULL REFERENCES community_proposal(id) ON DELETE CASCADE,
    user_id      BIGINT    NOT NULL,
    supported_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (proposal_id, user_id)
);
CREATE INDEX idx_proposal_support_proposal ON proposal_support(proposal_id);

CREATE TABLE community_ballot (
    id                       BIGSERIAL PRIMARY KEY,
    community_id             BIGINT       NOT NULL REFERENCES community(id),
    title                    TEXT         NOT NULL,
    category                 TEXT,
    description              TEXT,
    resolution_motion_text   TEXT,
    start_date               TIMESTAMP,
    end_date                 TIMESTAMP,
    total_eligible_voters    INT          DEFAULT 100,
    min_quorum_percentage    INT          DEFAULT 50,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_by               BIGINT
);
CREATE INDEX idx_community_ballot_community ON community_ballot(community_id);

CREATE TABLE ballot_option (
    id          BIGSERIAL PRIMARY KEY,
    ballot_id   BIGINT NOT NULL REFERENCES community_ballot(id) ON DELETE CASCADE,
    text        TEXT   NOT NULL,
    description TEXT
);
CREATE INDEX idx_ballot_option_ballot ON ballot_option(ballot_id);

CREATE TABLE ballot_vote (
    id           BIGSERIAL PRIMARY KEY,
    ballot_id    BIGINT    NOT NULL REFERENCES community_ballot(id) ON DELETE CASCADE,
    option_id    BIGINT    NOT NULL REFERENCES ballot_option(id),
    user_id      BIGINT    NOT NULL,
    receipt_hash TEXT      NOT NULL,
    voted_at     TIMESTAMP DEFAULT NOW(),
    UNIQUE (ballot_id, user_id)
);
CREATE INDEX idx_ballot_vote_ballot ON ballot_vote(ballot_id);

CREATE TABLE community_resolution (
    id                       BIGSERIAL PRIMARY KEY,
    community_id             BIGINT       NOT NULL REFERENCES community(id),
    resolution_number        TEXT         NOT NULL,
    title                    TEXT         NOT NULL,
    meeting_reference        TEXT,
    passed_date              DATE,
    category                 TEXT,
    approved_budget          TEXT,
    status                   VARCHAR(30)  NOT NULL DEFAULT 'PASSED',
    for_votes                INT          DEFAULT 0,
    against_votes            INT          DEFAULT 0,
    abstained_votes          INT          DEFAULT 0,
    presiding_officer        TEXT,
    legal_binding_statement  TEXT,
    document_url             TEXT,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_by               BIGINT
);
CREATE INDEX idx_community_resolution_community ON community_resolution(community_id);

CREATE TABLE community_resolution_provisions (
    resolution_id   BIGINT NOT NULL REFERENCES community_resolution(id) ON DELETE CASCADE,
    provision_text  TEXT
);
CREATE INDEX idx_resolution_provisions_resolution ON community_resolution_provisions(resolution_id);

CREATE TABLE governance_vault_document (
    id                      BIGSERIAL PRIMARY KEY,
    community_id            BIGINT       NOT NULL REFERENCES community(id),
    title                   TEXT         NOT NULL,
    category                VARCHAR(20)  NOT NULL DEFAULT 'MINUTES',
    file_size               TEXT,
    file_format             TEXT,
    document_url            TEXT,
    is_confidential         BOOLEAN      DEFAULT FALSE,
    resolution_ref          TEXT,
    digital_seal_verified   BOOLEAN      DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_by              BIGINT
);
CREATE INDEX idx_governance_vault_document_community ON governance_vault_document(community_id);
