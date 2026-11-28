package com.manacommunity.api.utilitymeter.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tanker_patrol_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TankerPatrolTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private PatrolScenario scenario;

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType;

    private String guardName;
    private String checkpointName;
    private double scannedLatitude;
    private double scannedLongitude;
    private double expectedLatitude;
    private double expectedLongitude;
    private double gpsDeviationMeters;
    private boolean gpsSpoofDetected;

    private String tankerVendor;
    private double entryWeightKg;
    private double exitWeightKg;
    private double deliveredLiters;
    private double invoicedLiters;
    private double volumeVariancePercent;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum PatrolScenario {
        QR_SCAN_VALID_LOCATION, NFC_SCAN_GPS_SPOOF_REJECTED,
        TANKER_WEIGHT_MATCHES_INVOICE, TANKER_VOLUME_MISMATCH,
        PATROL_MISSED_CHECKPOINT, GUARD_SCAN_OUTSIDE_GEOFENCE,
        DUPLICATE_SCAN_SAME_CHECKPOINT, TANKER_ENTRY_EXIT_TIME_MISMATCH
    }

    public enum VerificationType { QR_CODE, NFC, WEIGHT_BRIDGE, FLOW_METER }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
