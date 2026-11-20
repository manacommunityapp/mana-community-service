package com.manacommunity.api.platform.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalOutboxPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String aggregateType, String aggregateId, String eventType,
                        Object payload, Long communityId) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(json)
                    .communityId(communityId)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            // Serialisation failure is a programming error — propagate so the caller
            // knows the event was not enqueued.
            throw new IllegalArgumentException("Failed to serialise outbox payload for " + eventType, e);
        }
    }
}
