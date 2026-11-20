CREATE TYPE move_type AS ENUM ('MOVE_IN','MOVE_OUT');
CREATE TYPE move_status AS ENUM ('REQUESTED','APPROVED','IN_PROGRESS','COMPLETED','CANCELLED','REJECTED');

CREATE TABLE move_request (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT NOT NULL,
    resident_user_id BIGINT NOT NULL,
    flat_number      VARCHAR(20) NOT NULL,
    move_type        move_type NOT NULL,
    planned_date     DATE NOT NULL,
    planned_time     TIME,
    vehicle_number   VARCHAR(20),
    movers_company   VARCHAR(255),
    contact_number   VARCHAR(15),
    status           move_status NOT NULL DEFAULT 'REQUESTED',
    reviewed_by      BIGINT,
    reviewed_at      TIMESTAMP,
    review_notes     TEXT,
    noc_issued       BOOLEAN NOT NULL DEFAULT FALSE,
    noc_issued_at    TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE move_checklist_item (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    move_type       move_type NOT NULL,
    item_name       VARCHAR(255) NOT NULL,
    is_mandatory    BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   INT NOT NULL DEFAULT 0
);

CREATE TABLE move_checklist_completion (
    id                BIGSERIAL PRIMARY KEY,
    move_request_id   BIGINT NOT NULL REFERENCES move_request(id),
    checklist_item_id BIGINT NOT NULL REFERENCES move_checklist_item(id),
    completed         BOOLEAN NOT NULL DEFAULT FALSE,
    completed_by      BIGINT,
    completed_at      TIMESTAMP,
    notes             TEXT,
    UNIQUE(move_request_id, checklist_item_id)
);

INSERT INTO move_checklist_item (community_id, move_type, item_name, is_mandatory, display_order) VALUES
(0, 'MOVE_IN',  'Key handover',                     TRUE,  1),
(0, 'MOVE_IN',  'Welcome kit provided',              FALSE, 2),
(0, 'MOVE_IN',  'Society rules acknowledged',        TRUE,  3),
(0, 'MOVE_IN',  'Security deposit collected',        TRUE,  4),
(0, 'MOVE_IN',  'ID proof verified',                 TRUE,  5),
(0, 'MOVE_OUT', 'Key returned',                      TRUE,  1),
(0, 'MOVE_OUT', 'No pending dues',                   TRUE,  2),
(0, 'MOVE_OUT', 'Property inspection done',          TRUE,  3),
(0, 'MOVE_OUT', 'Security deposit refund processed', FALSE, 4),
(0, 'MOVE_OUT', 'NOC from RWA',                      TRUE,  5);

CREATE INDEX idx_move_comm ON move_request(community_id, status);
CREATE INDEX idx_move_res  ON move_request(resident_user_id);
