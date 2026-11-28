package com.manacommunity.api.whatsappbot.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dlt_template_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DltTemplateTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private DltScenario scenario;

    @Enumerated(EnumType.STRING)
    private ChannelType channel;

    private String templateName;
    private String dltEntityId;
    private String dltTemplateId;
    private boolean dltRegistered;

    private String language;
    private int variableCount;
    private int maxVariableChars;
    private int actualVariableChars;
    private boolean charLimitExceeded;

    private boolean deliverySuccessful;
    private double deliveryRatePercent;
    private String deliveryFailureReason;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum DltScenario {
        SMS_TEMPLATE_MATCH_CHECK, WHATSAPP_UTILITY_TEMPLATE_MATCH,
        VARIABLE_CHAR_LIMIT_EXCEEDED, UNREGISTERED_TEMPLATE_BLOCKED,
        REGIONAL_LANGUAGE_DELIVERY, TEMPLATE_ID_MISMATCH,
        PROMOTIONAL_VS_TRANSACTIONAL_CHECK, DLT_SCRUBBING_DELAY
    }

    public enum ChannelType { SMS, WHATSAPP, RCS }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
