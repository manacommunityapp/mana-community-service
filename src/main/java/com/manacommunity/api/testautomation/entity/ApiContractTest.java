package com.manacommunity.api.testautomation.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ta_api_contract_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiContractTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testSuiteCode;
    private String testSuiteName;

    @Enumerated(EnumType.STRING)
    private AutomationFramework framework;

    @Enumerated(EnumType.STRING)
    private TestLayer layer;

    private String serviceName;
    private String endpointOrTopic;
    private String httpMethod;
    private int totalAssertions;
    private int passedAssertions;
    private int failedAssertions;
    private long executionTimeMs;

    private boolean contractTestEnabled;
    private String consumerService;
    private String providerService;
    private boolean schemaBreakingChange;
    private String breakingChangeDescription;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;

    private String failureReason;

    public enum AutomationFramework { REST_ASSURED, PYTEST, SUPERTEST, PACT, KARATE, POSTMAN_NEWMAN }
    public enum TestLayer { API_REGRESSION, CONTRACT_CONSUMER, CONTRACT_PROVIDER, SCHEMA_VALIDATION, LOAD_TEST }
    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
