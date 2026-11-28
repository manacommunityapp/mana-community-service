package com.manacommunity.api.accesscontrol.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "access_biometric_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiometricTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private BiometricScenario scenario;

    private String gateId;
    private String gateName;
    private String subjectName;
    private String subjectRole;

    @Enumerated(EnumType.STRING)
    private RecognitionType recognitionType;

    private int turnstileSyncLatencyMs;
    private boolean multiGateSync;
    private int gatesPropagated;
    private int totalGates;
    private int propagationDelayMs;

    private boolean spoofAttempt;
    private String spoofMethod;
    private boolean spoofRejected;

    @Enumerated(EnumType.STRING)
    private LightingCondition lightingCondition;

    private double confidenceScore;
    private double thresholdScore;
    private boolean accessGranted;
    private boolean blacklisted;

    @Enumerated(EnumType.STRING)
    private SmartLockTest.TestResult testResult;

    private String failureReason;

    public enum BiometricScenario {
        FACE_RECOGNITION_NORMAL, FACE_RECOGNITION_LOW_LIGHT, FACE_RECOGNITION_BACKLIT,
        ANTI_SPOOF_PRINTED_PHOTO, ANTI_SPOOF_PHONE_SCREEN, MULTI_GATE_BLACKLIST_SYNC,
        TURNSTILE_SYNC_LATENCY, STAFF_ADD_PROPAGATION
    }

    public enum RecognitionType { FACIAL, FINGERPRINT, IRIS }
    public enum LightingCondition { NORMAL, LOW_LIGHT, BACKLIT, DIRECT_SUNLIGHT, NIGHT_IR }
}
