package com.manacommunity.api.resilience;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Thread-safe 3-state Circuit Breaker (CLOSED -> OPEN -> HALF_OPEN) using a
 * count-based sliding window. Distinguishes transient infrastructure faults
 * (5xx, timeouts, IO errors) from client 4xx errors so user validation failures
 * never trip the gateway circuit.
 */
public class GatewayCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String name;
    private final int slidingWindowSize;
    private final double failureRateThresholdPercent;
    private final Duration openCooldown;
    private final int halfOpenPermittedCalls;
    private final Clock clock;

    private final Deque<Boolean> window = new ArrayDeque<>();
    private State state = State.CLOSED;
    private long openedAtMillis = 0L;
    private int halfOpenAttempts = 0;
    private int halfOpenSuccesses = 0;

    public GatewayCircuitBreaker(
            String name,
            int slidingWindowSize,
            double failureRateThresholdPercent,
            Duration openCooldown,
            int halfOpenPermittedCalls,
            Clock clock) {
        this.name = name;
        this.slidingWindowSize = Math.max(2, slidingWindowSize);
        this.failureRateThresholdPercent = failureRateThresholdPercent;
        this.openCooldown = openCooldown;
        this.halfOpenPermittedCalls = Math.max(1, halfOpenPermittedCalls);
        this.clock = clock;
    }

    public synchronized State getState() {
        evaluateOpenToHalfOpen();
        return state;
    }

    public String getName() {
        return name;
    }

    public <T> T execute(Supplier<T> supplier) {
        acquirePermission();
        try {
            T result = supplier.get();
            onSuccess();
            return result;
        } catch (ClientBusinessException ex) {
            // 4xx business rejections (e.g. declined card, invalid VPA) do NOT count against breaker health
            onSuccess();
            throw ex;
        } catch (RuntimeException ex) {
            onFailure();
            throw ex;
        }
    }

    private synchronized void acquirePermission() {
        evaluateOpenToHalfOpen();
        if (state == State.OPEN) {
            throw new CircuitOpenException("Circuit breaker [" + name + "] is OPEN; failing fast");
        }
        if (state == State.HALF_OPEN) {
            if (halfOpenAttempts >= halfOpenPermittedCalls) {
                throw new CircuitOpenException("Circuit breaker [" + name + "] HALF_OPEN probe quota exhausted");
            }
            halfOpenAttempts++;
        }
    }

    private synchronized void onSuccess() {
        if (state == State.HALF_OPEN) {
            halfOpenSuccesses++;
            if (halfOpenSuccesses >= halfOpenPermittedCalls) {
                transitionToClosed();
            }
            return;
        }
        recordOutcome(true);
    }

    private synchronized void onFailure() {
        if (state == State.HALF_OPEN) {
            transitionToOpen();
            return;
        }
        recordOutcome(false);
        if (window.size() >= slidingWindowSize) {
            long failures = window.stream().filter(ok -> !ok).count();
            double failureRate = (failures * 100.0) / window.size();
            if (failureRate >= failureRateThresholdPercent) {
                transitionToOpen();
            }
        }
    }

    private void recordOutcome(boolean success) {
        if (window.size() >= slidingWindowSize) {
            window.removeFirst();
        }
        window.addLast(success);
    }

    private void evaluateOpenToHalfOpen() {
        if (state == State.OPEN && (clock.millis() - openedAtMillis) >= openCooldown.toMillis()) {
            state = State.HALF_OPEN;
            halfOpenAttempts = 0;
            halfOpenSuccesses = 0;
        }
    }

    private void transitionToOpen() {
        state = State.OPEN;
        openedAtMillis = clock.millis();
    }

    private void transitionToClosed() {
        state = State.CLOSED;
        window.clear();
        halfOpenAttempts = 0;
        halfOpenSuccesses = 0;
    }

    /** Thrown when failing fast on an OPEN circuit. */
    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }

    /** Marker exception for HTTP 4xx business errors that must not trip the circuit breaker. */
    public static class ClientBusinessException extends RuntimeException {
        public ClientBusinessException(String message) {
            super(message);
        }
    }
}
