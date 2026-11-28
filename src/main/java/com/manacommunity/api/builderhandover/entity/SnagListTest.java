package com.manacommunity.api.builderhandover.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snag_list_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SnagListTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private SnagScenario scenario;

    private String flatNumber;
    private String roomName;
    private String defectType;
    private String severity;
    private boolean photoAnnotated;
    private String assignedContractor;
    private String contractorTrade;
    private int slaDays;
    private int actualDays;
    private boolean slaBreached;
    private boolean reinspectionDone;
    private boolean signedOff;
    private boolean possessionBlocked;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum SnagScenario {
        CRACK_TILE_LOGGED_WITH_PHOTO, SEEPAGE_HIGH_SEVERITY_BLOCKS_POSSESSION,
        ELECTRICAL_FAULT_CONTRACTOR_ROUTED, SLA_BREACH_NO_ESCALATION,
        REINSPECTION_LOOP_3_ATTEMPTS, POSSESSION_CERT_BEFORE_SNAG_CLOSE,
        BULK_SNAG_IMPORT_CSV, CONTRACTOR_REJECT_REASSIGN
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
