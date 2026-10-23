-- Supplier Reliability: performance event tracking

CREATE TABLE supplier_performance_events (
    id            BIGSERIAL      PRIMARY KEY,
    supplier_id   BIGINT         NOT NULL,
    po_id         BIGINT,
    rfq_id        BIGINT,
    community_id  BIGINT         NOT NULL,
    event_type    VARCHAR(30)    NOT NULL,
    numeric_value DECIMAL(5,2),
    on_time_flag  BOOLEAN,
    notes         VARCHAR(500),
    recorded_by   VARCHAR(120),
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_perf_supplier  ON supplier_performance_events (supplier_id);
CREATE INDEX idx_perf_community ON supplier_performance_events (community_id);
CREATE INDEX idx_perf_type      ON supplier_performance_events (event_type);
