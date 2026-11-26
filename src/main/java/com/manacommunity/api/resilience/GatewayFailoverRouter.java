package com.manacommunity.api.resilience;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Coordinates automatic circuit-broken failover across payment gateways
 * (Razorpay <-> Cashfree) and telco IVR trunks (Primary <-> Secondary),
 * delivering graceful degradation when all third-party providers degrade.
 */
@Slf4j
@Service
public class GatewayFailoverRouter {

    private final Map<String, GatewayCircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public GatewayFailoverRouter() {
        this(Clock.systemUTC());
    }

    public GatewayFailoverRouter(Clock clock) {
        this.clock = clock;
    }

    public GatewayCircuitBreaker getBreaker(String providerKey) {
        return breakers.computeIfAbsent(providerKey, k ->
                new GatewayCircuitBreaker(k, 10, 50.0, Duration.ofSeconds(30), 3, clock));
    }

    public void registerBreaker(String providerKey, GatewayCircuitBreaker breaker) {
        breakers.put(providerKey, breaker);
    }

    /**
     * Executes a payment operation against {@code primaryGateway}, seamlessly
     * failing over to {@code fallbackGateway} if primary is OPEN or errors with 5xx/timeout.
     * Client 4xx rejections propagate immediately without triggering failover.
     */
    public <T> GatewayResult<T> executePaymentWithFailover(
            String primaryGateway,
            Supplier<T> primaryCall,
            String fallbackGateway,
            Supplier<T> fallbackCall) {
        return executeWithFailover(
                primaryGateway,
                primaryCall,
                fallbackGateway,
                fallbackCall,
                "MANUAL_UPI_QR_FALLBACK",
                "Online gateways temporarily busy. Display Community UPI QR or offline UTR entry."
        );
    }

    /**
     * Executes an e-Intercom / IVR trigger against primary SIP provider, failing over
     * to secondary telco, and degrading cleanly to Push + SMS OTP if both telcos are down.
     */
    public <T> GatewayResult<T> executeIvrWithFailover(
            Supplier<T> primaryIvrCall,
            Supplier<T> secondaryIvrCall) {
        return executeWithFailover(
                "IVR_PRIMARY",
                primaryIvrCall,
                "IVR_SECONDARY",
                secondaryIvrCall,
                "PUSH_SMS_OTP_FALLBACK",
                "Voice intercom carriers degraded; falling back to high-priority App Push + SMS OTP."
        );
    }

    private <T> GatewayResult<T> executeWithFailover(
            String primaryKey,
            Supplier<T> primaryCall,
            String fallbackKey,
            Supplier<T> fallbackCall,
            String degradedCode,
            String degradedMessage) {

        GatewayCircuitBreaker primary = getBreaker(primaryKey);
        try {
            T data = primary.execute(primaryCall);
            return GatewayResult.<T>builder()
                    .providerUsed(primaryKey)
                    .failoverTriggered(false)
                    .degradedMode(false)
                    .payload(data)
                    .build();
        } catch (GatewayCircuitBreaker.ClientBusinessException clientEx) {
            throw clientEx;
        } catch (RuntimeException primaryFault) {
            log.warn("[GatewayRouter] Primary provider {} failed ({}) — routing to fallback {}",
                    primaryKey, primaryFault.getMessage(), fallbackKey);
        }

        GatewayCircuitBreaker fallback = getBreaker(fallbackKey);
        try {
            T fallbackData = fallback.execute(fallbackCall);
            return GatewayResult.<T>builder()
                    .providerUsed(fallbackKey)
                    .failoverTriggered(true)
                    .degradedMode(false)
                    .payload(fallbackData)
                    .build();
        } catch (GatewayCircuitBreaker.ClientBusinessException clientEx) {
            throw clientEx;
        } catch (RuntimeException fallbackFault) {
            log.error("[GatewayRouter] Both {} and {} degraded ({}). Activating graceful degradation={}",
                    primaryKey, fallbackKey, fallbackFault.getMessage(), degradedCode);
            return GatewayResult.<T>builder()
                    .providerUsed(degradedCode)
                    .failoverTriggered(true)
                    .degradedMode(true)
                    .degradationReason(degradedMessage)
                    .build();
        }
    }

    @Data
    @Builder
    public static class GatewayResult<T> {
        private String providerUsed;
        private boolean failoverTriggered;
        private boolean degradedMode;
        private String degradationReason;
        private T payload;
    }
}
