package com.manacommunity.api.sosemergency.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sos_panic_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PanicSosTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private PanicScenario scenario;

    private String triggeredBy;
    private String location;
    private String signalStrength;
    private int signalBars;

    @Enumerated(EnumType.STRING)
    private FallbackChannel primaryChannel;

    @Enumerated(EnumType.STRING)
    private FallbackChannel fallbackChannel;

    private boolean httpDelivered;
    private int httpLatencyMs;
    private boolean smsFallbackTriggered;
    private boolean smsDelivered;
    private int smsLatencyMs;

    private boolean guardTabletSilentMode;
    private boolean sirenOverrideTriggered;
    private boolean sirenAudible;

    private int totalRecipientsTargeted;
    private int totalRecipientsReached;
    private int deliverySuccessPercent;

    @Enumerated(EnumType.STRING)
    private ChildExitGuardrailTest.TestResult testResult;

    private String failureReason;

    public enum PanicScenario {
        LOW_SIGNAL_ELEVATOR_SOS, NORMAL_SIGNAL_PANIC,
        SMS_FALLBACK_ON_HTTP_FAIL, GUARD_TABLET_SILENT_SIREN_OVERRIDE,
        MASS_SOS_LOAD_TEST, DUPLICATE_SOS_DEBOUNCE
    }

    public enum FallbackChannel { HTTP_PUSH, SMS, VOICE_CALL, SIREN, INTERCOM }
}
