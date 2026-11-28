package com.manacommunity.api.messaging.workload.model;

import lombok.Builder;
import lombok.Value;

/**
 * Architectural specification describing how a specific workload behaves and where it routes.
 */
@Value
@Builder
public class WorkloadProfile {
    WorkloadType workloadType;
    TargetBroker recommendedBroker;
    WorkloadPriority defaultPriority;
    boolean requiresStrictOrdering;
    boolean requiresReplay;
    boolean requiresDeduplication;
    int retentionDays; // 0 for transient/RabbitMQ, > 0 for Kafka
    String defaultDestination; // Topic name (Kafka) or Exchange name (RabbitMQ)
    String defaultRoutingKeyPattern;
    String rationale;
}
