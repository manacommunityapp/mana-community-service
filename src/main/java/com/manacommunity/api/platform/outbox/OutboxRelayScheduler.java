package com.manacommunity.api.platform.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingWithLock(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            try {
                // Deserialise to a generic map so listeners receive a structured payload.
                Object payload = objectMapper.readValue(event.getPayload(), Map.class);
                applicationEventPublisher.publishEvent(
                        new OutboxEventRelayed(event.getEventType(), event.getAggregateType(),
                                event.getAggregateId(), event.getCommunityId(), payload));
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setLastError(ex.getMessage());
                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event {} permanently failed after {} retries: {}",
                            event.getId(), retries, ex.getMessage());
                } else {
                    log.warn("Outbox event {} relay attempt {} failed: {}",
                            event.getId(), retries, ex.getMessage());
                }
            }
        }
    }
}
