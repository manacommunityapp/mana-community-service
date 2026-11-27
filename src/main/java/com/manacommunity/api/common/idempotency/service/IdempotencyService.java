package com.manacommunity.api.common.idempotency.service;

import com.manacommunity.api.common.idempotency.entity.FreshIdempotencyLog;
import com.manacommunity.api.common.idempotency.repository.FreshIdempotencyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Manages idempotency key reservation, collision detection, and cached response retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final FreshIdempotencyLogRepository repository;

    /**
     * Checks if a request with this idempotency key was already completed or is currently processing.
     * If not found, reserves the key with status 'PROCESSING'.
     *
     * @return Existing log if completed, or empty if new reservation succeeded.
     */
    @Transactional
    public Optional<FreshIdempotencyLog> startOrGet(
            String idempotencyKey,
            String requestHash,
            String endpointPath,
            String httpMethod,
            int ttlMinutes) {

        Optional<FreshIdempotencyLog> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            FreshIdempotencyLog logEntry = existing.get();
            if ("COMPLETED".equalsIgnoreCase(logEntry.getStatus())) {
                log.info("Idempotent replay: Key '{}' already completed for {} {}", idempotencyKey, httpMethod, endpointPath);
                return existing;
            }
            if ("PROCESSING".equalsIgnoreCase(logEntry.getStatus())) {
                log.warn("Concurrent idempotent request detected for key '{}'", idempotencyKey);
                return existing;
            }
        }

        Instant now = Instant.now();
        FreshIdempotencyLog newEntry = FreshIdempotencyLog.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .endpointPath(endpointPath)
                .httpMethod(httpMethod)
                .status("PROCESSING")
                .createdAt(now)
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .build();

        repository.save(newEntry);
        return Optional.empty();
    }

    /**
     * Records the completed HTTP response for an idempotency key.
     */
    @Transactional
    public void markCompleted(String idempotencyKey, int statusCode, String responseBody) {
        repository.findByIdempotencyKey(idempotencyKey).ifPresent(entry -> {
            entry.setStatus("COMPLETED");
            entry.setResponseStatus(statusCode);
            entry.setResponseBody(responseBody);
            repository.save(entry);
            log.debug("Recorded idempotency response for key '{}'", idempotencyKey);
        });
    }

    /**
     * Removes an in-flight key if an unexpected exception occurred, allowing the client to safely retry.
     */
    @Transactional
    public void releaseKey(String idempotencyKey) {
        try {
            repository.findByIdempotencyKey(idempotencyKey).ifPresent(entry -> {
                if ("PROCESSING".equalsIgnoreCase(entry.getStatus())) {
                    repository.delete(entry);
                    log.debug("Released in-flight idempotency key '{}' following error", idempotencyKey);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to release idempotency key '{}': {}", idempotencyKey, e.getMessage());
        }
    }
}
