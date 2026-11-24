package com.manacommunity.api.platform.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;
    private static final int SAFETY_SWEEP_CYCLES = 12; // Every 12 * 5s = 60s fallback safety sweep

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final AtomicInteger emptyCycles = new AtomicInteger(0);

    @Scheduled(fixedDelay = 5_000)
    public void relay() {
        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(TransactionalOutboxPublisher.OUTBOX_PENDING_KEY);
                long count = (val != null && !val.isBlank()) ? Long.parseLong(val.trim()) : 0L;
                if (count <= 0 && emptyCycles.incrementAndGet() < SAFETY_SWEEP_CYCLES) {
                    return; // Skip DB poll when Redis indicates 0 pending events
                }
            } catch (Exception e) {
                log.debug("Redis outbox count check failed, proceeding to DB check: {}", e.getMessage());
            }
        } else {
            // In-memory fallback: if idle for multiple cycles, sweep every 30s (6 cycles)
            if (emptyCycles.incrementAndGet() < 6) {
                return;
            }
        }

        int processed = processBatch();
        if (processed == 0) {
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(TransactionalOutboxPublisher.OUTBOX_PENDING_KEY, "0");
                } catch (Exception ignored) {}
            }
        } else {
            emptyCycles.set(0);
        }
    }

    @Transactional
    public int processBatch() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingWithLock(BATCH_SIZE);
        if (pending.isEmpty()) {
            return 0;
        }

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
        return pending.size();
    }
}
