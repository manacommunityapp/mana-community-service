package com.manacommunity.api.chaosresilience.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "backward_compatibility_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BackwardCompatibilityTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private CompatScenario scenario;

    private String oldAppVersion;
    private String currentApiVersion;
    private int monthsOld;

    private boolean jsonParsingSuccessful;
    private int unknownFieldsIgnored;
    private int missingFieldsHandled;
    private String parsingError;

    private boolean forceUpdatePromptShown;
    private boolean forceUpdateBlocking;
    private boolean appFunctionalAfterDismiss;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum CompatScenario {
        SIX_MONTH_OLD_APP_NEW_API, THREE_MONTH_OLD_NON_BLOCKING_UPDATE,
        CRITICAL_FORCE_UPDATE_BLOCKS, UNKNOWN_JSON_FIELDS_IGNORED,
        REMOVED_FIELD_GRACEFUL_FALLBACK, ENUM_VALUE_UNKNOWN_HANDLED,
        API_VERSION_HEADER_MISSING, DEPRECATED_ENDPOINT_REDIRECT
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
