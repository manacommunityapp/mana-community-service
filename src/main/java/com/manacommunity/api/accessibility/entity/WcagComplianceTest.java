package com.manacommunity.api.accessibility.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wcag_compliance_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WcagComplianceTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private WcagScenario scenario;

    private String pageName;
    private String wcagLevel;
    private String criterionId;
    private String criterionName;
    private boolean screenReaderCompatible;
    private boolean keyboardNavigable;
    private double contrastRatio;
    private double requiredContrast;
    private boolean altTextPresent;
    private boolean ariaLabelsCorrect;
    private boolean focusIndicatorVisible;
    private String toolUsed;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum WcagScenario {
        VISITOR_LOG_SCREEN_READER, PAYMENT_KEYBOARD_ONLY,
        DASHBOARD_CONTRAST_RATIO, GATE_PASS_ALT_TEXT,
        COMPLAINT_FORM_ARIA_LABELS, NOTICE_FOCUS_INDICATOR,
        EMERGENCY_SOS_VOICE_CONTROL, INTERCOM_COLOR_BLIND_SAFE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
