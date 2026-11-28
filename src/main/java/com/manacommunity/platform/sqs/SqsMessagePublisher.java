package com.manacommunity.platform.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Fail-safe AWS SQS publisher with automatic in-process fallback.
 *
 * <p>When {@code app.sqs.enabled=true} and an {@link SqsClient} bean is present, events are
 * serialized as {@link SqsEventEnvelope} JSON and sent to SQS. If SQS is disabled, unavailable,
 * or {@code sendMessage} throws, the supplied {@code localFallback} runs asynchronously on
 * {@code notificationExecutor} (or directly if no executor is provided) so business workflows
 * never lose events or block.</p>
 */
@Slf4j
@Service
public class SqsMessagePublisher {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final Executor fallbackExecutor;

    private final ConcurrentHashMap<String, String> queueUrlCache = new ConcurrentHashMap<>();

    public SqsMessagePublisher(
            @Autowired(required = false) SqsClient sqsClient,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper,
            @Autowired(required = false) @Qualifier("notificationExecutor") Executor fallbackExecutor) {
        this.sqsClient = sqsClient;
        this.sqsProperties = sqsProperties;
        this.objectMapper = objectMapper;
        this.fallbackExecutor = fallbackExecutor;
    }

    /**
     * Publishes an event to SQS after the current transaction commits (or immediately if no
     * transaction is active). Falls back to {@code localFallback} when SQS is disabled/unavailable.
     */
    public void publishAfterCommit(String queueName, String eventType, Object payload, Runnable localFallback) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(queueName, eventType, payload, localFallback);
                }
            });
        } else {
            publish(queueName, eventType, payload, localFallback);
        }
    }

    /**
     * Publishes an event immediately to SQS, or runs {@code localFallback} when SQS is disabled or fails.
     *
     * @return {@code true} if delivered to SQS, {@code false} if executed via local fallback.
     */
    public boolean publish(String queueName, String eventType, Object payload, Runnable localFallback) {
        if (sqsClient != null && sqsProperties != null && sqsProperties.isEnabled()) {
            try {
                String payloadJson = (payload instanceof String s)
                        ? s
                        : objectMapper.writeValueAsString(payload);
                SqsEventEnvelope envelope = new SqsEventEnvelope(
                        UUID.randomUUID().toString(),
                        eventType,
                        queueName,
                        Instant.now(),
                        payloadJson
                );
                String body = objectMapper.writeValueAsString(envelope);
                String queueUrl = SqsConfig.resolveQueueUrl(sqsClient, queueUrlCache, queueName);

                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        .build());
                log.debug("Published SQS event type={} queue={} eventId={}", eventType, queueName, envelope.eventId());
                return true;
            } catch (Exception e) {
                log.warn("SQS publish failed for queue={} type={}, executing local fallback: {}",
                        queueName, eventType, e.getMessage());
            }
        }

        runFallback(localFallback, queueName, eventType);
        return false;
    }

    private void runFallback(Runnable localFallback, String queueName, String eventType) {
        if (localFallback == null) return;
        Runnable safeTask = () -> {
            try {
                localFallback.run();
            } catch (Exception ex) {
                log.error("SQS local fallback failed for queue={} type={}: {}",
                        queueName, eventType, ex.getMessage(), ex);
            }
        };
        if (fallbackExecutor != null) {
            CompletableFuture.runAsync(safeTask, fallbackExecutor);
        } else {
            safeTask.run();
        }
    }
}
