-- CCTV / security camera registry

CREATE TABLE security_camera (
    id               BIGSERIAL    PRIMARY KEY,
    community_id     BIGINT       NOT NULL,
    name             VARCHAR(100),
    location         VARCHAR(200),
    type             VARCHAR(50),
    status           VARCHAR(20)  DEFAULT 'ONLINE',
    stream_url       VARCHAR(500),
    snapshot_url     VARCHAR(500),
    last_snapshot_at TIMESTAMP,
    install_date     DATE,
    ip_address       VARCHAR(50),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    DEFAULT NOW()
);
CREATE INDEX idx_security_camera_community ON security_camera(community_id);
