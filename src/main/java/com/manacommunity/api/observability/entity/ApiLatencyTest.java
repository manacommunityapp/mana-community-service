package com.manacommunity.api.observability.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_latency_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiLatencyTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private LatencyScenario scenario;

    private String endpoint;
    private String httpMethod;
    private int p50Ms;
    private int p95Ms;
    private int p99Ms;
    private int slaMs;
    private boolean slaMet;
    private int requestCount;
    private int errorCount;
    private double errorRate;
    private boolean tracingEnabled;
    private String traceId;
    private boolean slowQueryDetected;
    private int slowQueryMs;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum LatencyScenario {
        DASHBOARD_LOAD_P95, VISITOR_LOG_API_SLOW_QUERY,
        PAYMENT_CALLBACK_TIMEOUT, BULK_INVOICE_GENERATION,
        REAL_TIME_GATE_RESPONSE, NOTIFICATION_FANOUT_LATENCY,
        DATABASE_CONNECTION_POOL_EXHAUSTED, CDN_CACHE_MISS_SPIKE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
