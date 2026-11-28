package com.manacommunity.api.whatsappbot.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bot_session_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BotSessionTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private SessionScenario scenario;

    private String conversationFlow;
    private int sessionTimeoutMinutes;
    private int actualGapMinutes;
    private boolean sessionExpired;
    private boolean stateRecovered;

    private String userInput;
    private boolean invalidInputHandled;
    private String botResponse;

    private boolean pdfReceiptGenerated;
    private String paymentMethod;
    private boolean upiCallbackReceived;
    private int receiptGenerationDelayMs;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum SessionScenario {
        PAY_DUES_SESSION_EXPIRED_45MIN, INVALID_TEXT_INPUT_HANDLED,
        PDF_RECEIPT_AFTER_UPI_CALLBACK, MID_FLOW_APP_SWITCH,
        SESSION_RECOVERY_AFTER_TIMEOUT, CONCURRENT_SESSIONS_SAME_USER,
        BOT_HANDOFF_TO_HUMAN_AGENT, RECEIPT_DOWNLOAD_LINK_EXPIRY
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
