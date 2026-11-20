-- Workflow definitions (reference data for allowed transitions)
CREATE TABLE workflow_definition (
    id           VARCHAR(60)  PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE workflow_transition_rule (
    id              BIGSERIAL    PRIMARY KEY,
    workflow_id     VARCHAR(60)  NOT NULL REFERENCES workflow_definition(id),
    from_status     VARCHAR(60)  NOT NULL,
    to_status       VARCHAR(60)  NOT NULL,
    allowed_roles   TEXT         NOT NULL,  -- comma-separated role names
    reason_required BOOLEAN      NOT NULL DEFAULT FALSE,
    display_label   VARCHAR(100),
    sort_order      SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE(workflow_id, from_status, to_status)
);

-- Full audit trail of every status transition across all modules
CREATE TABLE workflow_transition_history (
    id              BIGSERIAL    PRIMARY KEY,
    workflow_id     VARCHAR(60)  NOT NULL,
    entity_type     VARCHAR(60)  NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    community_id    BIGINT       NOT NULL,
    from_status     VARCHAR(60)  NOT NULL,
    to_status       VARCHAR(60)  NOT NULL,
    actor_user_id   BIGINT,
    actor_role      VARCHAR(30),
    reason          TEXT,
    metadata        JSONB,
    idempotency_key VARCHAR(200) UNIQUE,
    transitioned_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wth_entity    ON workflow_transition_history(entity_type, entity_id);
CREATE INDEX idx_wth_community ON workflow_transition_history(community_id, transitioned_at DESC);
CREATE INDEX idx_wth_actor     ON workflow_transition_history(actor_user_id);

-- Seed four reference workflow definitions
INSERT INTO workflow_definition(id, display_name) VALUES
  ('BOOKING',            'Service Booking'),
  ('ORDER',              'Commerce Order'),
  ('TRIP_BOOKING',       'Trip Booking'),
  ('MAINTENANCE_TICKET', 'Maintenance Ticket');

-- BOOKING transitions
INSERT INTO workflow_transition_rule(workflow_id, from_status, to_status, allowed_roles, reason_required, display_label, sort_order) VALUES
  ('BOOKING','DRAFT',       'PENDING',      'RESIDENT,ADMIN,SUPER_ADMIN',         false,'Submit',          1),
  ('BOOKING','PENDING',     'CONFIRMED',    'VENDOR,ADMIN,SUPER_ADMIN',           false,'Confirm',         1),
  ('BOOKING','PENDING',     'CANCELLED',    'RESIDENT,VENDOR,ADMIN,SUPER_ADMIN',true, 'Cancel',          2),
  ('BOOKING','CONFIRMED',   'IN_PROGRESS',  'VENDOR,ADMIN,SUPER_ADMIN',           false,'Start Service',   1),
  ('BOOKING','CONFIRMED',   'CANCELLED',    'RESIDENT,VENDOR,ADMIN,SUPER_ADMIN',true, 'Cancel',          2),
  ('BOOKING','IN_PROGRESS', 'COMPLETED',    'VENDOR,ADMIN,SUPER_ADMIN,RESIDENT',false,'Mark Complete',   1),
  ('BOOKING','IN_PROGRESS', 'CANCELLED',    'ADMIN,SUPER_ADMIN',                   true, 'Cancel',          2),
  ('BOOKING','COMPLETED',   'REFUNDED',     'ADMIN,SUPER_ADMIN',                   true, 'Process Refund',  1),
  ('BOOKING','CANCELLED',   'REFUNDED',     'ADMIN,SUPER_ADMIN',                   true, 'Process Refund',  1);

-- ORDER transitions
INSERT INTO workflow_transition_rule(workflow_id, from_status, to_status, allowed_roles, reason_required, display_label, sort_order) VALUES
  ('ORDER','CREATED',         'ACCEPTED',         'VENDOR,ADMIN,SUPER_ADMIN',          false,'Accept Order',     1),
  ('ORDER','CREATED',         'CANCELLED',        'RESIDENT,VENDOR,ADMIN,SUPER_ADMIN',true, 'Cancel',           2),
  ('ORDER','ACCEPTED',        'PROCESSING',       'VENDOR,ADMIN,SUPER_ADMIN',          false,'Start Processing', 1),
  ('ORDER','ACCEPTED',        'CANCELLED',        'VENDOR,ADMIN,SUPER_ADMIN',          true, 'Cancel',           2),
  ('ORDER','PROCESSING',      'READY',            'VENDOR,ADMIN,SUPER_ADMIN',          false,'Mark Ready',       1),
  ('ORDER','READY',           'OUT_FOR_DELIVERY', 'VENDOR,GUARD,ADMIN,SUPER_ADMIN',  false,'Dispatch',         1),
  ('ORDER','READY',           'COMPLETED',        'VENDOR,ADMIN,SUPER_ADMIN',          false,'Pickup Complete',  2),
  ('ORDER','OUT_FOR_DELIVERY','DELIVERED',        'GUARD,VENDOR,ADMIN,SUPER_ADMIN',  false,'Mark Delivered',   1),
  ('ORDER','DELIVERED',       'COMPLETED',        'RESIDENT,VENDOR,ADMIN,SUPER_ADMIN',false,'Confirm Receipt', 1),
  ('ORDER','DELIVERED',       'DISPUTED',         'RESIDENT',                              true, 'Raise Dispute',    2),
  ('ORDER','COMPLETED',       'REFUNDED',         'ADMIN,SUPER_ADMIN',                   true, 'Process Refund',   1),
  ('ORDER','DISPUTED',        'REFUNDED',         'ADMIN,SUPER_ADMIN',                   true, 'Process Refund',   1),
  ('ORDER','DISPUTED',        'COMPLETED',        'ADMIN,SUPER_ADMIN',                   true, 'Resolve Dispute',  2);

-- TRIP_BOOKING transitions
INSERT INTO workflow_transition_rule(workflow_id, from_status, to_status, allowed_roles, reason_required, display_label, sort_order) VALUES
  ('TRIP_BOOKING','PENDING',    'CONFIRMED',  'ADMIN,SUPER_ADMIN',                   false,'Confirm Seat',    1),
  ('TRIP_BOOKING','PENDING',    'CANCELLED',  'RESIDENT,ADMIN,SUPER_ADMIN',         true, 'Cancel',          2),
  ('TRIP_BOOKING','CONFIRMED',  'IN_PROGRESS','ADMIN,SUPER_ADMIN',                   false,'Trip Departed',   1),
  ('TRIP_BOOKING','CONFIRMED',  'CANCELLED',  'RESIDENT,ADMIN,SUPER_ADMIN',         true, 'Cancel',          2),
  ('TRIP_BOOKING','IN_PROGRESS','COMPLETED',  'ADMIN,SUPER_ADMIN',                   false,'Trip Completed',  1),
  ('TRIP_BOOKING','COMPLETED',  'REFUNDED',   'ADMIN,SUPER_ADMIN',                   true, 'Process Refund',  1),
  ('TRIP_BOOKING','CANCELLED',  'REFUNDED',   'ADMIN,SUPER_ADMIN',                   true, 'Process Refund',  1);

-- MAINTENANCE_TICKET transitions
INSERT INTO workflow_transition_rule(workflow_id, from_status, to_status, allowed_roles, reason_required, display_label, sort_order) VALUES
  ('MAINTENANCE_TICKET','OPEN',          'IN_PROGRESS',    'STAFF,ADMIN,SUPER_ADMIN',          false,'Assign & Start',   1),
  ('MAINTENANCE_TICKET','OPEN',          'CANCELLED',      'RESIDENT,ADMIN,SUPER_ADMIN',        true, 'Cancel',           2),
  ('MAINTENANCE_TICKET','IN_PROGRESS',   'PENDING_REVIEW', 'STAFF,ADMIN,SUPER_ADMIN',          false,'Submit for Review', 1),
  ('MAINTENANCE_TICKET','IN_PROGRESS',   'CANCELLED',      'ADMIN,SUPER_ADMIN',                  true, 'Cancel',           2),
  ('MAINTENANCE_TICKET','PENDING_REVIEW','RESOLVED',       'RESIDENT,ADMIN,SUPER_ADMIN',        false,'Confirm Resolved', 1),
  ('MAINTENANCE_TICKET','PENDING_REVIEW','IN_PROGRESS',    'RESIDENT,ADMIN,SUPER_ADMIN',        true, 'Reopen',           2),
  ('MAINTENANCE_TICKET','RESOLVED',      'CLOSED',         'ADMIN,SUPER_ADMIN',                  false,'Close',            1),
  ('MAINTENANCE_TICKET','RESOLVED',      'REOPENED',       'RESIDENT,ADMIN,SUPER_ADMIN',        true, 'Reopen',           1),
  ('MAINTENANCE_TICKET','REOPENED',      'IN_PROGRESS',    'STAFF,ADMIN,SUPER_ADMIN',          false,'Resume Work',      1),
  ('MAINTENANCE_TICKET','CLOSED',        'REOPENED',       'ADMIN,SUPER_ADMIN',                  true, 'Reopen',           1);
