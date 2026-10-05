package com.manacommunity.api.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Audit recorder for infrastructure resilience, circuit breaker trips, retry exhaustion,
 * and service degradations across critical modules (Wallet, Finance, Emergency, Parking, etc.).
 * Guarantees zero fake fallback by recording exact failure signatures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilienceAuditService {

    private final AuditService auditService;

    /**
     * Records a circuit breaker trip or external outage.
     *
     * @param module     target business domain (e.g. WALLET, PAYMENTS, EMERGENCY)
     * @param action     failure action (e.g. CIRCUIT_BREAKER_TRIP, GATEWAY_TIMEOUT)
     * @param entityId   target entity ID or order ID
     * @param rootCause  underlying error message (sanitized)
     */
    public void recordFailure(AuditModule module, AuditAction action, String entityName, String entityId, String rootCause) {
        log.warn("Resilience event: module={} action={} entityName={} entityId={} cause={}",
                module, action, entityName, entityId, rootCause);
        auditService.record(action, module, entityName, entityId, null, "FAILED: " + rootCause);
    }

    /**
     * Records retry exhaustion before raising a friendly 503 error to the client.
     */
    public void recordRetryExhausted(AuditModule module, String operationName, String referenceId, int attempts, String finalError) {
        String detail = String.format("Attempts: %d | Final Error: %s", attempts, finalError);
        log.error("Retry exhausted for {} [{}]: {}", module, operationName, detail);
        auditService.record(AuditAction.RETRY_EXHAUSTED, module, operationName, referenceId, null, detail);
    }
}
