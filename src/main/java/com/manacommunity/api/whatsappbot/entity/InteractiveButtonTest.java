package com.manacommunity.api.whatsappbot.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interactive_button_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteractiveButtonTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private ButtonScenario scenario;

    private String visitorName;
    private String whatsappAction;
    private String mobileAppAction;
    private int actionDelayMs;
    private boolean concurrentActions;

    private boolean singleWinnerLocked;
    private String winningAction;
    private String losingAction;
    private boolean duplicateWebhookReceived;
    private int webhookCount;
    private boolean webhookDeduped;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum ButtonScenario {
        APPROVE_WA_DENY_APP_CONCURRENT, APPROVE_BOTH_CHANNELS,
        DENY_WA_APPROVE_APP_CONCURRENT, DUPLICATE_META_WEBHOOK,
        TRIPLE_TAP_APPROVE, WEBHOOK_TIMEOUT_RETRY,
        STALE_BUTTON_AFTER_RESOLUTION, FAMILY_MEMBER_CONFLICT
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
