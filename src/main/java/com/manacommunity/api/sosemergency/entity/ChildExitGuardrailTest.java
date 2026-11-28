package com.manacommunity.api.sosemergency.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sos_child_exit_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildExitGuardrailTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private ChildExitScenario scenario;

    private String childName;
    private int childAge;
    private String flatNumber;
    private String requestedBy;
    private String requestedByRole;
    private boolean primaryParentOnlyLock;
    private boolean overrideAttempted;
    private boolean overrideBlocked;
    private String overrideAttemptedBy;

    private boolean ivrTriggered;
    private int ivrTimeoutSeconds;
    private boolean ivrAnswered;
    private String ivrCalledNumber;
    private boolean backupContactTriggered;
    private String backupContactName;
    private boolean autoAlertToGuard;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;

    private String failureReason;

    public enum ChildExitScenario {
        UNACCOMPANIED_ALERT, PRIMARY_PARENT_LOCK_OVERRIDE_ATTEMPT,
        IVR_TIMEOUT_BACKUP_CALL, IVR_ANSWERED_APPROVE, IVR_UNANSWERED_AUTO_DENY,
        GRANDPARENT_AUTHORIZED_PICKUP, UNAUTHORIZED_PERSON_ATTEMPT
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
