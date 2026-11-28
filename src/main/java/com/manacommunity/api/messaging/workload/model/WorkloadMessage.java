package com.manacommunity.api.messaging.workload.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Universal message envelope routed across Kafka or RabbitMQ.
 */
@Value
@Builder(toBuilder = true)
public class WorkloadMessage {
    String messageId;
    String societyId;
    WorkloadType workloadType;
    WorkloadPriority priority;
    String partitionOrRoutingKey;
    String correlationId;
    Map<String, Object> headers;
    String payloadJson;
    @Builder.Default
    Instant timestamp = Instant.now();
    @Builder.Default
    int retryCount = 0;
    @Builder.Default
    long ttlMs = 0L; // 0 indicates no per-message TTL
}
