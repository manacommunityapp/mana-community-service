package com.manacommunity.api.messaging.workload.service;

import com.manacommunity.api.messaging.workload.model.RoutingDecision;
import com.manacommunity.api.messaging.workload.model.RoutingResult;
import com.manacommunity.api.messaging.workload.model.TargetBroker;
import com.manacommunity.api.messaging.workload.model.WorkloadMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Handles dead-lettering, exponential retry backoff, and poison-pill quarantine
 * across Kafka (DLT) and RabbitMQ (DLX).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterAndRetryEngine {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000L;
    private static final long MAX_DELAY_MS = 30000L;

    private final KafkaMessageBusAdapter kafkaAdapter;
    private final RabbitMQMessageBusAdapter rabbitMQAdapter;
    private final Random random = new Random();

    /**
     * Determines whether a message should be retried or routed to dead-letter storage.
     */
    public boolean shouldRetry(int currentRetryCount) {
        return currentRetryCount < DEFAULT_MAX_RETRIES;
    }

    /**
     * Calculates exponential backoff with full jitter:
     * sleep = rand(0, min(MAX_DELAY, BASE_DELAY * 2^retryCount))
     */
    public long calculateBackoffMs(int retryCount) {
        long exponentialDelay = BASE_DELAY_MS * (1L << Math.min(retryCount, 10));
        long cappedDelay = Math.min(exponentialDelay, MAX_DELAY_MS);
        return 100L + (long) (random.nextDouble() * (cappedDelay - 100L));
    }

    /**
     * Routes an unrecoverable or exhausted message to its dead-letter repository.
     */
    public RoutingResult deadLetter(WorkloadMessage message, RoutingDecision decision, String failureReason) {
        log.warn("[DEAD-LETTER] Routing poison message to dead-letter store. MsgId={} Type={} TargetBroker={} Reason='{}'",
                message.getMessageId(), message.getWorkloadType(), decision.getTargetBroker(), failureReason);

        if (decision.getTargetBroker() == TargetBroker.KAFKA) {
            String dltTopic = decision.getDeadLetterDestination();
            return kafkaAdapter.publishToDlt(message, dltTopic, failureReason);
        } else {
            String dlxExchange = decision.getDeadLetterDestination();
            String dlxRoutingKey = "dead-letter." + message.getSocietyId() + "." + message.getWorkloadType().name().toLowerCase();
            return rabbitMQAdapter.publishToDlx(message, dlxExchange, dlxRoutingKey, failureReason);
        }
    }
}
