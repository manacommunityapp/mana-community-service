package com.manacommunity.api.messaging.workload.model;

import lombok.Builder;
import lombok.Value;

/**
 * Encapsulates the broker routing resolution for a workload message.
 */
@Value
@Builder
public class RoutingDecision {
    TargetBroker targetBroker;
    String destination; // Kafka topic or RabbitMQ exchange
    String routingKey;
    String partitionKey;
    WorkloadPriority priority;
    boolean dlqEnabled;
    String deadLetterDestination; // DLT topic or DLX exchange
    int deliveryTimeoutMs;
    String rationale;
}
