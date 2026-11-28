package com.manacommunity.api.tenantkyc.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "move_out_revocation_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MoveOutRevocationTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private RevocationScenario scenario;

    private String flatNumber;
    private String tenantName;
    private boolean appAccessRevoked;
    private boolean guestPasscodesInvalidated;
    private int activePasscodesBeforeMoveOut;
    private int activePasscodesAfterMoveOut;
    private boolean rfidStickersDeactivated;
    private boolean dailyHelpUnlinked;
    private boolean gatePassBlocked;
    private boolean maintenanceDuesPending;
    private double pendingDuesAmount;
    private int revocationDelayMs;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum RevocationScenario {
        CLEAN_MOVE_OUT_ALL_REVOKED, MOVE_OUT_WITH_PENDING_DUES,
        GUEST_PASSCODE_LEAK_AFTER_MOVE_OUT, RFID_STILL_ACTIVE_POST_MOVE_OUT,
        DAILY_HELP_RETAINED_AFTER_MOVE_OUT, GATE_PASS_BLOCKED_UNPAID_DUES,
        CONCURRENT_MOVE_OUT_MOVE_IN, APP_SESSION_TOKEN_STILL_VALID
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
