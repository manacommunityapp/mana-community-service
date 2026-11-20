CREATE TYPE pass_type AS ENUM ('DELIVERY','DOMESTIC_WORKER','GUEST','VEHICLE','CONTRACTOR');
CREATE TYPE pass_status AS ENUM ('ACTIVE','USED','EXPIRED','REVOKED');

CREATE TABLE digital_pass (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    created_by      BIGINT NOT NULL,
    flat_number     VARCHAR(20) NOT NULL,
    pass_type       pass_type NOT NULL,
    visitor_name    VARCHAR(255),
    vehicle_number  VARCHAR(20),
    purpose         VARCHAR(500),
    token           VARCHAR(64) NOT NULL UNIQUE,
    status          pass_status NOT NULL DEFAULT 'ACTIVE',
    valid_from      TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until     TIMESTAMP NOT NULL,
    used_at         TIMESTAMP,
    used_by_guard   BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dp_community ON digital_pass(community_id, status, valid_until);
CREATE INDEX idx_dp_token     ON digital_pass(token);
CREATE INDEX idx_dp_creator   ON digital_pass(created_by, valid_until);
