package com.manacommunity.api.perfbenchmark.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "load_test_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoadTestResult extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private LoadScenario scenario;

    private String endpointPath;
    private int concurrentUsers;
    private int requestsPerSecond;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double targetP95Ms;
    private double errorRate;
    private double throughputRps;
    private String toolUsed;
    private int durationMinutes;
    private boolean slaBreached;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum LoadScenario {
        DASHBOARD_100_CONCURRENT, VISITOR_LOG_PEAK_500_RPS,
        PAYMENT_GATEWAY_TIMEOUT, GATE_OPEN_API_REALTIME,
        NOTIFICATION_BURST_1000, DB_CONNECTION_POOL_EXHAUST,
        FILE_UPLOAD_LARGE_PHOTO, WEBSOCKET_INTERCOM_SCALE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
