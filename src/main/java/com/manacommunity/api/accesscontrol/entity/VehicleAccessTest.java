package com.manacommunity.api.accesscontrol.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "access_vehicle_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleAccessTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private VehicleScenario scenario;

    private String vehicleNumber;
    private String rfidTagId;
    private String gateId;
    private String gateName;
    private String basementLevel;

    @Enumerated(EnumType.STRING)
    private EventType lastEvent;

    @Enumerated(EnumType.STRING)
    private EventType attemptedEvent;

    private boolean antiPassbackTriggered;
    private boolean entryAllowed;

    private int visitorParkingSpotsTotal;
    private int visitorParkingSpotsUsed;
    private boolean capacityLimitEnforced;
    private boolean overflowRedirected;
    private String redirectedTo;

    private int rfidReadDistanceCm;
    private int rfidReadTimeMs;
    private boolean boomBarrierOpened;
    private int boomBarrierResponseMs;

    @Enumerated(EnumType.STRING)
    private SmartLockTest.TestResult testResult;

    private String failureReason;

    public enum VehicleScenario {
        ANTI_PASSBACK_DOUBLE_ENTRY, ANTI_PASSBACK_NORMAL_EXIT,
        VISITOR_PARKING_AT_CAPACITY, VISITOR_PARKING_OVERFLOW_REDIRECT,
        RFID_READ_RANGE_TEST, BOOM_BARRIER_RESPONSE_TIME,
        UNREGISTERED_VEHICLE_ENTRY, EXPIRED_VISITOR_TAG
    }

    public enum EventType { ENTRY, EXIT, NONE }
}
