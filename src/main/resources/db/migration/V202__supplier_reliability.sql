CREATE TABLE supplier_performance_events (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   BIGINT       NOT NULL,
    po_id         BIGINT,
    rfq_id        BIGINT,
    community_id  BIGINT       NOT NULL,
    event_type    VARCHAR(30)  NOT NULL,
    numeric_value DECIMAL(5,2),
    on_time_flag  BOOLEAN,
    notes         VARCHAR(500),
    recorded_by   VARCHAR(120),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_perf_supplier  (supplier_id),
    INDEX idx_perf_community (community_id),
    INDEX idx_perf_type      (event_type)
);
