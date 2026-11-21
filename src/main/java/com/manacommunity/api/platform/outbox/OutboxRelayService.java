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

    private final OutboxEventRepository repo;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messaging;

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void relay() {
        List<OutboxEvent> due = repo.findDueEvents(Instant.now());
        for (OutboxEvent event : due) {
            event.setStatus("PROCESSING");
            repo.save(event);
            try {
                dispatch(event);
                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
            } catch (Exception e) {
                log.warn("Outbox relay failed for event {} (attempt {}): {}", event.getId(), event.getRetryCount() + 1, e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= event.getMaxRetries()) {
                    event.setStatus("FAILED");
                    event.setFailedAt(Instant.now());
                    event.setErrorMessage(e.getMessage());
                } else {
                    event.setStatus("RETRY");
                    // Exponential backoff: 2^retryCount * 30 seconds
                    long backoffSeconds = (long) Math.pow(2, event.getRetryCount()) * 30;
                    event.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
                }
            }
            repo.save(event);
        }
    }

    private void dispatch(OutboxEvent event) {
        // Route to WebSocket topic based on event type
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
