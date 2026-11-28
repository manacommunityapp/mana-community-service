package com.manacommunity.platform.sqs;

/**
 * Contract implemented by each domain SQS queue consumer.
 * Implementations are polled automatically by {@link SqsQueuePoller} when
 * {@code app.sqs.enabled=true}.
 */
public interface SqsMessageHandler {

    /**
     * @return logical queue name (or queue URL) owned by this handler.
     */
    String queueName();

    /**
     * Processes one decoded {@link SqsEventEnvelope}. Throwing an exception leaves
     * the message on the SQS queue for visibility-timeout retry / DLQ delivery.
     */
    void handle(SqsEventEnvelope envelope) throws Exception;
}
