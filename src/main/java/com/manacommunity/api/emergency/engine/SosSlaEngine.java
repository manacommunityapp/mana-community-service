package com.manacommunity.api.emergency.engine;

import com.manacommunity.api.emergency.entity.SosIncident;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SosSlaEngine {

    /**
     * Determines target response SLA in seconds based on incident severity.
     */
    public int getTargetResponseSeconds(SosIncident.Severity severity) {
        if (severity == null) return 180;
        return switch (severity) {
            case CRITICAL -> 180; // 3 minutes
            case HIGH -> 300;     // 5 minutes
            case MEDIUM -> 600;   // 10 minutes
            case LOW -> 900;      // 15 minutes
        };
    }

    /**
     * Computes elapsed response time and determines if SLA was breached.
     */
    public SlaEvaluationResult evaluateResponseSla(LocalDateTime triggeredAt, LocalDateTime arrivedAt, int targetSeconds) {
        if (triggeredAt == null || arrivedAt == null) {
            return new SlaEvaluationResult(0, false);
        }

        long elapsedSeconds = Math.max(0, Duration.between(triggeredAt, arrivedAt).getSeconds());
        boolean isBreached = elapsedSeconds > targetSeconds;

        return new SlaEvaluationResult(elapsedSeconds, isBreached);
    }

    /**
     * Checks if acknowledgment from guards is overdue (e.g. within 60s for critical incidents).
     */
    public boolean isAcknowledgmentOverdue(LocalDateTime triggeredAt, LocalDateTime now, int maxAckSeconds) {
        if (triggeredAt == null || now == null) return false;
        long elapsed = Duration.between(triggeredAt, now).getSeconds();
        return elapsed > maxAckSeconds;
    }

    public record SlaEvaluationResult(long elapsedSeconds, boolean isBreached) {}
}