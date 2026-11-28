package com.manacommunity.api.messaging.workload.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Execution receipt returned after dispatching a workload message across the dual-bus.
 */
@Value
@Builder
public class RoutingResult {
    String messageId;
    WorkloadType workloadType;
    TargetBroker dispatchedBroker;
    boolean success;
    List<String> deliveredDestinations;
    String partitionAssigned;
    int priorityDelivered;
    Instant dispatchedAt;
    String errorMessage;
}
