package com.manacommunity.api.tenantkyc.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_lift_slot_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceLiftSlotTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private SlotScenario scenario;

    private String towerName;
    private int maxMoveInsPerDay;
    private int currentBookings;
    private String requestedSlot;
    private String freightElevatorWindow;

    private boolean bookingAccepted;
    private boolean overlapDetected;
    private String overlapWith;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum SlotScenario {
        WITHIN_QUOTA_ACCEPTED, QUOTA_EXCEEDED_REJECTED, OVERLAPPING_SLOT_BLOCKED,
        OUTSIDE_FREIGHT_WINDOW, WEEKEND_RESTRICTION, CANCEL_AND_REBOOK,
        CONCURRENT_BOOKING_RACE, HOLIDAY_BLACKOUT_DATE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
