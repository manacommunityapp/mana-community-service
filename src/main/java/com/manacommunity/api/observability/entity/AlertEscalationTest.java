package com.manacommunity.api.observability.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alert_escalation_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertEscalationTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private AlertScenario scenario;

    private String alertName;
    private String severity;
    private String channel;
    private int triggerThreshold;
    private int actualValue;
    private boolean alertFired;
    private int alertDelaySeconds;
    private boolean escalated;
    private String escalatedTo;
    private int escalationDelayMinutes;
    private boolean acknowledged;
    private boolean resolved;
    private int mttrMinutes;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum AlertScenario {
        CPU_THRESHOLD_ALERT_FIRED, DISK_FULL_NOT_ESCALATED,
        PAYMENT_FAILURE_SPIKE_PAGERDUTY, ERROR_RATE_SLACK_NOTIFICATION,
        ALERT_FATIGUE_DEDUP_CHECK, ONCALL_ROTATION_COVERAGE,
        MTTR_SLA_BREACH, SILENT_FAILURE_NO_ALERT
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
