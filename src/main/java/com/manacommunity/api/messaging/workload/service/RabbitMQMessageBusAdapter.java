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
 * Adapter encapsulating RabbitMQ publishing semantics:
 * - AMQP Exchange routing (Direct, Topic, Fanout)
 * - Priority tagging (0 - 10)
 * - Per-message TTL and delayed delivery headers
 * - Dead Letter Exchange (DLX) forwarding
 */
@Slf4j
@Service
public class RabbitMQMessageBusAdapter {

    private final AtomicLong deliveryTagCounter = new AtomicLong(1);
    private final Map<String, List<WorkloadMessage>> exchangeStore = new ConcurrentHashMap<>();
    private final Map<String, List<WorkloadMessage>> deadLetterStore = new ConcurrentHashMap<>();

    /**
     * Publishes a message to an AMQP exchange with routing key, priority and TTL headers.
     */
    public RoutingResult publish(WorkloadMessage message, RoutingDecision decision) {
        String exchange = decision.getDestination();
        String routingKey = decision.getRoutingKey();
        int priority = decision.getPriority().getAmqpPriorityLevel();
        long deliveryTag = deliveryTagCounter.incrementAndGet();

        // Simulate AMQP message properties
        Map<String, Object> amqpHeaders = new HashMap<>(message.getHeaders() != null ? message.getHeaders() : Map.of());
        amqpHeaders.put("x-priority", priority);
        if (message.getTtlMs() > 0) {
            amqpHeaders.put("x-message-ttl", message.getTtlMs());
        }
        amqpHeaders.put("x-delivery-tag", deliveryTag);

        WorkloadMessage enrichedMessage = message.toBuilder()
                .headers(amqpHeaders)
                .build();

        exchangeStore.computeIfAbsent(exchange, k -> Collections.synchronizedList(new ArrayList<>())).add(enrichedMessage);

        log.info("[RABBITMQ-PUBLISH] Exchange='{}' RoutingKey='{}' Priority={} Tag={} MsgId={} Type={}",
                exchange, routingKey, priority, deliveryTag, message.getMessageId(), message.getWorkloadType());

        return RoutingResult.builder()
                .messageId(message.getMessageId())
                .workloadType(message.getWorkloadType())
                .dispatchedBroker(TargetBroker.RABBITMQ)
                .success(true)
                .deliveredDestinations(List.of(exchange + "::" + routingKey))
                .partitionAssigned("AMQP-channel-1@tag-" + deliveryTag)
                .priorityDelivered(priority)
                .dispatchedAt(Instant.now())
                .build();
    }

    /**
     * Publishes to Dead Letter Exchange (DLX).
     */
    public RoutingResult publishToDlx(WorkloadMessage message, String dlxExchange, String routingKey, String reason) {
        long deliveryTag = deliveryTagCounter.incrementAndGet();
        Map<String, Object> dlxHeaders = new HashMap<>(message.getHeaders() != null ? message.getHeaders() : Map.of());
        dlxHeaders.put("x-death-reason", reason);
        dlxHeaders.put("x-death-timestamp", Instant.now().toString());

        WorkloadMessage deadLetterMessage = message.toBuilder()
                .headers(dlxHeaders)
                .build();

        deadLetterStore.computeIfAbsent(dlxExchange, k -> Collections.synchronizedList(new ArrayList<>())).add(deadLetterMessage);

        log.warn("[RABBITMQ-DLX] Forwarded to DLX='{}' RoutingKey='{}' MsgId={} Reason='{}'",
                dlxExchange, routingKey, message.getMessageId(), reason);

        return RoutingResult.builder()
                .messageId(message.getMessageId())
                .workloadType(message.getWorkloadType())
                .dispatchedBroker(TargetBroker.RABBITMQ)
                .success(true)
                .deliveredDestinations(List.of(dlxExchange + "::" + routingKey))
                .partitionAssigned("DLX-tag-" + deliveryTag)
                .priorityDelivered(0)
                .dispatchedAt(Instant.now())
                .errorMessage(reason)
                .build();
    }

    public int getRecordedMessageCount(String exchange) {
        List<WorkloadMessage> messages = exchangeStore.get(exchange);
        return messages != null ? messages.size() : 0;
    }

    public List<WorkloadMessage> getRecordedMessages(String exchange) {
        List<WorkloadMessage> messages = exchangeStore.get(exchange);
        return messages != null ? new ArrayList<>(messages) : List.of();
    }

    public List<WorkloadMessage> getDeadLetterMessages(String dlxExchange) {
        List<WorkloadMessage> messages = deadLetterStore.get(dlxExchange);
        return messages != null ? new ArrayList<>(messages) : List.of();
    }
}
