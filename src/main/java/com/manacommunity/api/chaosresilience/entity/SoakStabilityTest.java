package com.manacommunity.api.chaosresilience.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "soak_stability_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SoakStabilityTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private SoakScenario scenario;

    private String deviceModel;
    private int testDurationHours;
    private int simulatedVisitorEntries;
    private int qrScansProcessed;

    private long memoryStartMb;
    private long memoryEndMb;
    private long memoryPeakMb;
    private boolean memoryLeakDetected;
    private double memoryGrowthRatePerHour;

    private double batteryTempStartCelsius;
    private double batteryTempPeakCelsius;
    private boolean batteryOverheatDetected;

    private boolean sqliteCorrupted;
    private int sqliteIntegrityErrors;
    private int appCrashes;
    private int anrEvents;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum SoakScenario {
        GUARD_TABLET_12HR_SOAK, RESIDENT_APP_8HR_BACKGROUND,
        CAMERA_QR_CONTINUOUS_SCAN, OFFLINE_QUEUE_GROWTH_24HR,
        BLUETOOTH_BLE_CONTINUOUS_SCAN, WEBVIEW_MEMORY_LEAK_CHECK,
        NOTIFICATION_FLOOD_1000_IN_1HR, DATABASE_WAL_GROWTH_CHECK
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
