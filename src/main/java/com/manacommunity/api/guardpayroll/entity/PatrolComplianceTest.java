package com.manacommunity.api.guardpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patrol_compliance_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatrolComplianceTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private PatrolScenario scenario;

    private String guardName;
    private String routeName;
    private int totalCheckpoints;
    private int scannedCheckpoints;
    private int missedCheckpoints;
    private boolean gpsTrackingEnabled;
    private boolean routeDeviationDetected;
    private double deviationMeters;
    private String scanMethod;
    private boolean nightVisionPhotoTaken;
    private boolean panicButtonTested;
    private int patrolDurationMinutes;
    private int expectedDurationMinutes;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum PatrolScenario {
        ALL_CHECKPOINTS_SCANNED, MISSED_CHECKPOINT_ALERT,
        GPS_ROUTE_DEVIATION_DETECTED, NFC_SCAN_SEQUENCE_WRONG,
        NIGHT_PATROL_PHOTO_MISSING, PANIC_BUTTON_SOS_TRIGGERED,
        PATROL_DURATION_EXCEEDED, CHECKPOINT_SCAN_GPS_MISMATCH
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
