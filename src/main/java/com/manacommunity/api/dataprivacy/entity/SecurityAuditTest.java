package com.manacommunity.api.dataprivacy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "security_audit_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityAuditTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private AuditScenario scenario;

    private String endpoint;
    private String httpMethod;
    private String attackVector;
    private boolean inputSanitized;
    private boolean sqlInjectionBlocked;
    private boolean xssBlocked;
    private boolean rateLimitEnforced;
    private int requestsPerMinute;
    private int rateLimitThreshold;
    private boolean jwtValidated;
    private boolean roleEscalationBlocked;
    private String vulnerabilitySeverity;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum AuditScenario {
        SQL_INJECTION_VISITOR_SEARCH, XSS_NOTICEBOARD_POST,
        RATE_LIMIT_OTP_ENDPOINT, JWT_EXPIRY_TOKEN_REUSE,
        ROLE_ESCALATION_RESIDENT_TO_ADMIN, IDOR_FLAT_DATA_ACCESS,
        API_KEY_ROTATION_CHECK, BRUTE_FORCE_LOGIN_LOCKOUT
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
