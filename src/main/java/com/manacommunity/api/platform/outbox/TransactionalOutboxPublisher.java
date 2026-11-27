package com.manacommunity.api.platform.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalOutboxPublisher implements DomainEventPublisher {

    public static final String OUTBOX_PENDING_KEY = "outbox:pending_count";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

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
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().increment(OUTBOX_PENDING_KEY);
                } catch (Exception e) {
                    log.debug("Failed to increment outbox count in Redis: {}", e.getMessage());
                }
            }
        } catch (JsonProcessingException e) {
            // Serialisation failure is a programming error — propagate so the caller
            // knows the event was not enqueued.
            throw new IllegalArgumentException("Failed to serialise outbox payload for " + eventType, e);
        }
    }
}
