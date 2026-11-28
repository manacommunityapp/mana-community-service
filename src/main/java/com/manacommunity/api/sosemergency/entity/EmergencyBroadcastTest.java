package com.manacommunity.api.sosemergency.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sos_broadcast_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyBroadcastTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private BroadcastScenario scenario;

    @Enumerated(EnumType.STRING)
    private EmergencyType emergencyType;

    private String affectedTower;
    private int residentsInTower;
    private int staffInTower;
    private int activeVisitorsInTower;
    private int totalTargeted;
    private int totalReached;
    private int falsePositives;
    private boolean geoFencingAccurate;

    private boolean nonAffectedTowerExcluded;
    private int nonAffectedResidentsWronglyAlerted;

    private int broadcastLatencyMs;
    private String channelUsed;

    @Enumerated(EnumType.STRING)
    private ChildExitGuardrailTest.TestResult testResult;

    private String failureReason;

    public enum BroadcastScenario {
        TOWER_SPECIFIC_EVACUATION, FULL_SOCIETY_ALERT,
        GEO_TARGETED_FIRE_ALARM, VISITOR_INCLUDED_IN_ALERT,
        STAFF_ONLY_ALERT, FALSE_POSITIVE_DRILL
    }

    public enum EmergencyType { FIRE, GAS_LEAK, EARTHQUAKE, FLOODING, SECURITY_BREACH, MEDICAL, DRILL }
}
