package com.manacommunity.api.observability.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "release_telemetry_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReleaseTelemetryTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private TelemetryScenario scenario;

    private String releaseVersion;
    private String previousVersion;
    private int rolloutPercent;
    private int crashCount;
    private double crashRate;
    private double previousCrashRate;
    private boolean crashRateRegression;
    private int anrCount;
    private double anrRate;
    private boolean featureFlagEnabled;
    private String featureFlagName;
    private int adoptionPercent;
    private boolean autoRollbackTriggered;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum TelemetryScenario {
        STAGED_ROLLOUT_5_PERCENT, CRASH_RATE_REGRESSION_DETECTED,
        ANR_SPIKE_ON_NEW_VERSION, FEATURE_FLAG_GRADUAL_ROLLOUT,
        AUTO_ROLLBACK_THRESHOLD, ADOPTION_CURVE_TRACKING,
        HOTFIX_RELEASE_METRICS, CANARY_DEPLOYMENT_COMPARISON
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
