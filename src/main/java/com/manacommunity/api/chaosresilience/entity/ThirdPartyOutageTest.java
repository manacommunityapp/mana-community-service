package com.manacommunity.api.chaosresilience.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "third_party_outage_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThirdPartyOutageTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private OutageScenario scenario;

    private String serviceName;
    private int injectedHttpStatus;
    private int injectedDelayMs;
    private String injectedErrorType;

    @Enumerated(EnumType.STRING)
    private FallbackChannel fallbackChannel;
    private boolean fallbackTriggered;
    private int fallbackLatencyMs;

    private boolean userErrorMessageGraceful;
    private String userErrorMessage;
    private boolean dataLossPrevented;
    private boolean retryAttempted;
    private int retryCount;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum OutageScenario {
        FCM_PUSH_DELAY_30S, RAZORPAY_502_ERROR, SMS_GATEWAY_TIMEOUT,
        TALLY_EXPORT_CONNECTION_REFUSED, S3_UPLOAD_TIMEOUT,
        GOOGLE_MAPS_QUOTA_EXCEEDED, AADHAAR_OCR_SERVICE_DOWN,
        INTERCOM_SIP_TRUNK_FAILURE
    }

    public enum FallbackChannel { VOIP_CALL, IVR_CALL, SMS, EMAIL, IN_APP_NOTIFICATION, NONE }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
