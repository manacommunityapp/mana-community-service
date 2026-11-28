package com.manacommunity.api.megatownship.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "federated_gate_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FederatedGateTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private GateScenario scenario;

    private String sourceTower;
    private String destinationTower;
    private String visitorType;
    private boolean crossTowerPassValid;
    private boolean centralGateSync;
    private int syncDelayMs;
    private boolean rfidRecognizedAcrossTowers;
    private boolean anprCameraMatched;
    private String vehicleNumber;
    private boolean boomBarrierOpened;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum GateScenario {
        CROSS_TOWER_VISITOR_PASS, CENTRAL_GATE_MULTI_TOWER_SYNC,
        RFID_RECOGNIZED_DIFFERENT_TOWER, ANPR_VEHICLE_WRONG_TOWER_PARKING,
        DELIVERY_AGENT_MULTI_TOWER_ROUTE, GUEST_PASS_FEDERATION_EXPIRED,
        EMERGENCY_VEHICLE_ALL_GATES_OPEN, BOOM_BARRIER_SYNC_FAILURE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
