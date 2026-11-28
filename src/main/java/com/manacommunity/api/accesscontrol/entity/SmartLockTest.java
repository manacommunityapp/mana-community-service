package com.manacommunity.api.accesscontrol.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "access_smart_lock_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmartLockTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private LockScenario scenario;

    private String lockId;
    private String lockModel;
    private String flatNumber;

    @Enumerated(EnumType.STRING)
    private PinType pinType;

    private String generatedPin;
    private String validFrom;
    private String validUntil;
    private int clockDriftToleranceSec;
    private int actualClockDriftSec;
    private boolean internetConnected;
    private boolean pinAccepted;
    private boolean boundaryMinuteTest;
    private boolean earlyAccessBlocked;
    private boolean expiredAccessBlocked;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;

    private String failureReason;

    public enum LockScenario {
        TOTP_NO_INTERNET, BLUETOOTH_PIN_OFFLINE, EXACT_START_BOUNDARY,
        EXACT_EXPIRY_BOUNDARY, CLOCK_DRIFT_30SEC, CLOCK_DRIFT_5MIN,
        EXPIRED_PIN_REUSE, CONCURRENT_PIN_GENERATION
    }

    public enum PinType { TOTP, BLUETOOTH_PIN, NFC, QR_CODE }
    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
