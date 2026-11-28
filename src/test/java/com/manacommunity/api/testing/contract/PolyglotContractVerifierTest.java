package com.manacommunity.api.testing.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotContractVerifierTest {

    private PolyglotContractVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new PolyglotContractVerifierImpl();
    }

    @Test
    void testFullComplianceAcrossPolyglotConsumers() {
        // Provider GateApprovalService produces this Kafka payload:
        String providerPayload = """
            {
              "eventId": "evt-8821",
              "societyId": 101,
              "visitorId": 5001,
              "unitNumber": "Tower B - 402",
              "gateCode": "GATE_NORTH_01",
              "approvalStatus": "APPROVED",
              "approvalLatencyMs": 1420,
              "durationSeconds": 3600,
              "timestamp": "2026-11-28T10:00:00Z"
            }
            """;

        // Consumer 1: BillingService (Java)
        PactMessageContract billingContract = PactMessageContract.builder()
                .consumerName("BillingService-Java")
                .providerName("GateApprovalService-Java")
                .interactionName("visitor access approved event")
                .fieldExpectations(List.of(
                        ContractFieldExpectation.builder().fieldName("societyId").expectedType("NUMBER").required(true).build(),
                        ContractFieldExpectation.builder().fieldName("visitorId").expectedType("NUMBER").required(true).build(),
                        ContractFieldExpectation.builder().fieldName("durationSeconds").expectedType("NUMBER").required(true).build()
                ))
                .build();

        // Consumer 2: AnalyticsService (Python)
        PactMessageContract analyticsContract = PactMessageContract.builder()
                .consumerName("AnalyticsService-Python")
                .providerName("GateApprovalService-Java")
                .interactionName("visitor access approved event")
                .fieldExpectations(List.of(
                        ContractFieldExpectation.builder().fieldName("societyId").expectedType("NUMBER").required(true).build(),
                        ContractFieldExpectation.builder().fieldName("gateCode").expectedType("STRING").required(true).build(),
                        ContractFieldExpectation.builder().fieldName("approvalLatencyMs").expectedType("NUMBER").required(true).build()
                ))
                .build();

        // Consumer 3: NotificationService (Node.js)
        PactMessageContract notificationContract = PactMessageContract.builder()
                .consumerName("NotificationService-Node")
                .providerName("GateApprovalService-Java")
                .interactionName("visitor access approved event")
                .fieldExpectations(List.of(
                        ContractFieldExpectation.builder().fieldName("visitorId").expectedType("NUMBER").required(true).build(),
                        ContractFieldExpectation.builder().fieldName("approvalStatus").expectedType("STRING").required(true)
                                .regexPattern("^(APPROVED|DENIED|AUTO_WHITELIST)$").build()
                ))
                .build();

        List<ContractValidationResult> results = verifier.verifyPayload(
                "GateApprovalService-Java", providerPayload, 
                List.of(billingContract, analyticsContract, notificationContract));

        assertEquals(3, results.size());
        assertTrue(results.get(0).isCompliant());
        assertTrue(results.get(1).isCompliant());
        assertTrue(results.get(2).isCompliant());
        assertTrue(verifier.canDeploy(results), "can-i-deploy must pass when all consumers are satisfied");
    }

    @Test
    void testBreakingChangeFieldRenameBlocksDeployment() {
        // Provider changed 'visitorId' to 'guestId' in payload
        String breakingPayload = """
            {
              "eventId": "evt-8821",
              "societyId": 101,
              "guestId": 5001,
              "gateCode": "GATE_NORTH_01"
            }
            """;

        PactMessageContract billingContract = PactMessageContract.builder()
                .consumerName("BillingService-Java")
                .providerName("GateApprovalService-Java")
                .fieldExpectations(List.of(
                        ContractFieldExpectation.builder().fieldName("visitorId").expectedType("NUMBER").required(true).build()
                ))
                .build();

        List<ContractValidationResult> results = verifier.verifyPayload(
                "GateApprovalService-Java", breakingPayload, List.of(billingContract));

        assertEquals(1, results.size());
        assertFalse(results.get(0).isCompliant());
        assertTrue(results.get(0).getErrors().get(0).contains("visitorId"));
        assertFalse(verifier.canDeploy(results), "can-i-deploy must block deployment when required field is missing");
    }

    @Test
    void testTypeMismatchAndRegexFailure() {
        // Provider emitted gateCode as NUMBER instead of STRING and invalid status
        String invalidPayload = """
            {
              "societyId": 101,
              "gateCode": 101,
              "approvalStatus": "UNKNOWN_STATE"
            }
            """;

        PactMessageContract contract = PactMessageContract.builder()
                .consumerName("AnalyticsService-Python")
                .fieldExpectations(List.of(
                        ContractFieldExpectation.builder().fieldName("gateCode").expectedType("STRING").required(true).build(),
                        ContractFieldExpectation.builder().fieldName("approvalStatus").expectedType("STRING").required(true)
                                .regexPattern("^(APPROVED|DENIED)$").build()
                ))
                .build();

        List<ContractValidationResult> results = verifier.verifyPayload(
                "GateApprovalService-Java", invalidPayload, List.of(contract));

        assertEquals(1, results.size());
        assertFalse(results.get(0).isCompliant());
        assertEquals(2, results.get(0).getErrors().size());
        assertFalse(verifier.canDeploy(results));
    }
}
