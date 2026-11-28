package com.manacommunity.api.guardpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shift_handover_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftHandoverTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private HandoverScenario scenario;

    private String outgoingGuard;
    private String incomingGuard;
    private String shiftType;
    private String scheduledHandoverTime;
    private String actualHandoverTime;
    private int delayMinutes;
    private boolean checklistCompleted;
    private int checklistItems;
    private int itemsVerified;
    private boolean keyHandoverLogged;
    private boolean patrolRouteTransferred;
    private boolean incidentLogReviewed;
    private boolean gpsLocationVerified;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum HandoverScenario {
        CLEAN_SHIFT_CHANGE_8AM, DELAYED_HANDOVER_NO_REPLACEMENT,
        INCOMPLETE_CHECKLIST_FORCED, KEY_HANDOVER_NOT_LOGGED,
        PATROL_ROUTE_NOT_TRANSFERRED, GPS_SPOOF_AT_HANDOVER,
        DOUBLE_SHIFT_OVERTIME_ALERT, EMERGENCY_MID_SHIFT_HANDOVER
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
