package com.manacommunity.api.messaging.workload.service;

import com.manacommunity.api.messaging.workload.model.RoutingDecision;
import com.manacommunity.api.messaging.workload.model.RoutingResult;
import com.manacommunity.api.messaging.workload.model.TargetBroker;
import com.manacommunity.api.messaging.workload.model.WorkloadMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adapter encapsulating Apache Kafka publishing semantics:
 * - Deterministic partition hashing: abs(hash(partitionKey)) % partitionCount
 * - In-flight message tracking and offset simulation
 * - Append-only record log persistence
 */
@Slf4j
@Service
public class KafkaMessageBusAdapter {

    private static final int DEFAULT_PARTITIONS = 32;

    private final AtomicLong offsetCounter = new AtomicLong(1000);
    private final Map<String, List<WorkloadMessage>> topicLogStore = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> topicOffsets = new ConcurrentHashMap<>();

    /**
     * Publishes a message to the Kafka distributed commit log.
     */
    public RoutingResult publish(WorkloadMessage message, RoutingDecision decision) {
        String topic = decision.getDestination();
        String partitionKey = decision.getPartitionKey();
        int partitionCount = resolvePartitionCount(message);

        // Murmur2 / Hash partition assignment
        int partition = Math.abs(partitionKey.hashCode()) % partitionCount;
        long offset = topicOffsets.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();

        topicLogStore.computeIfAbsent(topic, k -> Collections.synchronizedList(new ArrayList<>())).add(message);

        log.info("[KAFKA-PRODUCE] Topic={} Partition={} Offset={} Key={} MsgId={} Type={}",
                topic, partition, offset, partitionKey, message.getMessageId(), message.getWorkloadType());

        return RoutingResult.builder()
                .messageId(message.getMessageId())
                .workloadType(message.getWorkloadType())
                .dispatchedBroker(TargetBroker.KAFKA)
                .success(true)
                .deliveredDestinations(List.of(topic))
                .partitionAssigned("partition-" + partition + "@offset-" + offset)
                .priorityDelivered(decision.getPriority().getAmqpPriorityLevel())
                .dispatchedAt(Instant.now())
                .build();
    }

    /**
     * Publishes to Dead Letter Topic (DLT) when poison pill or processing failures occur.
     */
    public RoutingResult publishToDlt(WorkloadMessage message, String dltTopic, String reason) {
        long offset = topicOffsets.computeIfAbsent(dltTopic, k -> new AtomicLong(0)).incrementAndGet();
        topicLogStore.computeIfAbsent(dltTopic, k -> Collections.synchronizedList(new ArrayList<>())).add(message);

        log.warn("[KAFKA-DLT] Published to DLT Topic={} Offset={} MsgId={} Reason={}",
                dltTopic, offset, message.getMessageId(), reason);

        return RoutingResult.builder()
                .messageId(message.getMessageId())
                .workloadType(message.getWorkloadType())
                .dispatchedBroker(TargetBroker.KAFKA)
                .success(true)
                .deliveredDestinations(List.of(dltTopic))
                .partitionAssigned("DLT-partition-0@offset-" + offset)
                .priorityDelivered(0)
                .dispatchedAt(Instant.now())
                .errorMessage(reason)
                .build();
    }

    public int getRecordedMessageCount(String topic) {
        List<WorkloadMessage> messages = topicLogStore.get(topic);
        return messages != null ? messages.size() : 0;
    }

    public List<WorkloadMessage> getRecordedMessages(String topic) {
        List<WorkloadMessage> messages = topicLogStore.get(topic);
        return messages != null ? new ArrayList<>(messages) : List.of();
    }

    private int resolvePartitionCount(WorkloadMessage message) {
        // High throughput batch topics allocate 128 partitions
        return switch (message.getWorkloadType()) {
            case MONTHLY_INVOICE_BATCH, GATE_ANPR_STREAM -> 128;
            case WATER_METER_TELEMETRY, BANK_SETTLEMENT_RECONCILIATION -> 64;
            default -> DEFAULT_PARTITIONS;
        };
    }
}
