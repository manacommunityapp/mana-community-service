package com.manacommunity.api.platform.outbox;

/**
 * Contract for publishing domain events reliably via the transactional outbox.
 * Implementations persist the event inside the caller's existing transaction so
 * the event is never lost if the business operation commits but a downstream
 * notification fails.
 */
public interface DomainEventPublisher {

    void publish(String aggregateType, String aggregateId, String eventType, Object payload, Long communityId);

    default void publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        publish(aggregateType, aggregateId, eventType, payload, null);
    }
}
