package com.manacommunity.api.testing.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Consumer-driven Pact specification for an asynchronous Kafka message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PactMessageContract {
    private String consumerName;      // e.g. "BillingService-Java", "AnalyticsService-Python", "NotificationService-Node"
    private String providerName;      // e.g. "GateApprovalService-Java"
    private String interactionName;   // e.g. "a visitor access approved event"
    private String topic;             // e.g. "visitor-access-approved"
    private String environmentTag;    // e.g. "production", "staging"
    private List<ContractFieldExpectation> fieldExpectations;
}
