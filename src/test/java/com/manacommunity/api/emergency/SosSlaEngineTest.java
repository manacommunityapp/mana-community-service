package com.manacommunity.api.emergency;

import com.manacommunity.api.emergency.engine.SosSlaEngine;
import com.manacommunity.api.emergency.engine.SosSlaEngine.SlaEvaluationResult;
import com.manacommunity.api.emergency.entity.SosIncident.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Emergency SOS SLA Engine Unit Tests")
public class SosSlaEngineTest {

    private final SosSlaEngine engine = new SosSlaEngine();

    @Test
    @DisplayName("Should assign 180 seconds SLA for CRITICAL severity")
    void shouldAssignCriticalSla() {
        assertThat(engine.getTargetResponseSeconds(Severity.CRITICAL)).isEqualTo(180);
    }

    @Test
    @DisplayName("Should assign 300 seconds SLA for HIGH severity")
    void shouldAssignHighSla() {
        assertThat(engine.getTargetResponseSeconds(Severity.HIGH)).isEqualTo(300);
    }

    @Test
    @DisplayName("Should evaluate response as compliant when arrived within SLA target")
    void shouldEvaluateCompliantResponse() {
        LocalDateTime triggeredAt = LocalDateTime.of(2026, 10, 1, 10, 0, 0);
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 10, 1, 10, 2, 30); // 150 seconds

        SlaEvaluationResult result = engine.evaluateResponseSla(triggeredAt, arrivedAt, 180);

        assertThat(result.elapsedSeconds()).isEqualTo(150);
        assertThat(result.isBreached()).isFalse();
    }

    @Test
    @DisplayName("Should evaluate response as breached when arrival exceeds SLA target")
    void shouldEvaluateBreachedResponse() {
        LocalDateTime triggeredAt = LocalDateTime.of(2026, 10, 1, 10, 0, 0);
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 10, 1, 10, 4, 10); // 250 seconds (Target 180)

        SlaEvaluationResult result = engine.evaluateResponseSla(triggeredAt, arrivedAt, 180);

        assertThat(result.elapsedSeconds()).isEqualTo(250);
        assertThat(result.isBreached()).isTrue();
    }

    @Test
    @DisplayName("Should detect when guard acknowledgment is overdue")
    void shouldDetectOverdueAcknowledgment() {
        LocalDateTime triggeredAt = LocalDateTime.of(2026, 10, 1, 10, 0, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 1, 15); // 75 seconds later

        assertThat(engine.isAcknowledgmentOverdue(triggeredAt, now, 60)).isTrue();
    }
}