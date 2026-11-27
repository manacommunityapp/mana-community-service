package com.manacommunity.api.platform.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final DomainEventPublisher domainEventPublisher;

    public void publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        domainEventPublisher.publish(aggregateType, aggregateId, eventType, payload, null);
    }
}
