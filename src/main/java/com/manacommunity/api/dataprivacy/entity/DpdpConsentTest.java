package com.manacommunity.api.dataprivacy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dpdp_consent_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DpdpConsentTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private ConsentScenario scenario;

    private String dataCategory;
    private String consentPurpose;
    private boolean consentCollected;
    private boolean consentWithdrawable;
    private boolean dataMinimized;
    private boolean retentionPolicyEnforced;
    private int retentionDays;
    private int actualRetentionDays;
    private boolean erasureCompleted;
    private boolean childDataProtected;
    private int childAge;
    private boolean parentalConsentObtained;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum ConsentScenario {
        RESIDENT_CONSENT_COLLECTION, CONSENT_WITHDRAWAL_DATA_DELETED,
        DATA_MINIMIZATION_CHECK, RETENTION_PERIOD_EXCEEDED,
        RIGHT_TO_ERASURE_REQUEST, CHILD_DATA_PARENTAL_CONSENT,
        THIRD_PARTY_SHARING_CONSENT, CONSENT_AUDIT_TRAIL_COMPLETE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
