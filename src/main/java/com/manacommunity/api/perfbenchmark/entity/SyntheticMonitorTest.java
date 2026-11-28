package com.manacommunity.api.perfbenchmark.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "synthetic_monitor_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyntheticMonitorTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private MonitorScenario scenario;

    private String monitorName;
    private String monitorType;
    private String targetUrl;
    private int checkIntervalMin;
    private double uptimePercent;
    private double targetUptimePercent;
    private int incidentCount;
    private double avgResponseMs;
    private boolean sslCertValid;
    private int sslExpiryDays;
    private boolean alertConfigured;
    private String alertChannel;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum MonitorScenario {
        API_HEALTH_CHECK_1MIN, LOGIN_FLOW_SYNTHETIC,
        PAYMENT_END_TO_END_FLOW, SSL_CERT_EXPIRY_WARNING,
        DNS_RESOLUTION_LATENCY, CDN_CACHE_HIT_RATIO,
        THIRD_PARTY_GATEWAY_DOWN, MULTI_REGION_FAILOVER
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
