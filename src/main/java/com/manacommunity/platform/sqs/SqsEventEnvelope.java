package com.manacommunity.platform.sqs;

import java.time.Instant;

/**
 * Standard envelope stored in every SQS message body across the platform.
 *
 * @param eventId     unique message/event ID for traceability & idempotency
 * @param eventType   domain event discriminator (e.g. {@code SMS_DISPATCH}, {@code OUTBID})
 * @param queueName   logical queue name
 * @param occurredAt  event creation timestamp
 * @param payloadJson JSON-serialized domain payload
 */
public record SqsEventEnvelope(
        String eventId,
        String eventType,
        String queueName,
        Instant occurredAt,
        String payloadJson
) {}
