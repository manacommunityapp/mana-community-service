package com.manacommunity.api.platform.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository repo;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messaging;

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void relay() {
        List<OutboxEvent> due = repo.findPendingWithLock(BATCH_SIZE);
        for (OutboxEvent event : due) {
            try {
                dispatch(event);
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
            } catch (Exception e) {
                log.warn("Outbox relay failed for event {} (attempt {}): {}",
                        event.getId(), event.getRetryCount() + 1, e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    event.setLastError(e.getMessage());
                }
                // else stays PENDING for the next polling cycle
            }
            repo.save(event);
        }
    }

    private void dispatch(OutboxEvent event) {
        try {
            Map<?, ?> payload = objectMapper.readValue(event.getPayload(), Map.class);
            String topic = "/topic/outbox/" + event.getAggregateType().toLowerCase();
            messaging.convertAndSend(topic, payload);
            log.debug("Dispatched outbox event {} to {}", event.getId(), topic);
        } catch (Exception e) {
            throw new RuntimeException("Dispatch failed: " + e.getMessage(), e);
        }
    }
}
