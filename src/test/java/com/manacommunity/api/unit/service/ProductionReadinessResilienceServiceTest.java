package com.manacommunity.api.unit.service;

import com.manacommunity.api.opsresilience.service.ProductionReadinessResilienceService;
import com.manacommunity.api.opsresilience.service.ProductionReadinessResilienceService.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Production Readiness: Outage Fallback, Force-Update & 12-Hour Soak Monitor Tests")
class ProductionReadinessResilienceServiceTest {

    private final ProductionReadinessResilienceService service = new ProductionReadinessResilienceService();

    @Test
    @DisplayName("Falls back from delayed FCM push to VoIP/IVR, handles SMS timeout, and trips Razorpay 502 circuit breaker")
    void shouldDegradeGracefullyDuringThirdPartyOutages() {
        var fcmDelayed = service.resolveNotificationFallback(OutageChannel.FCM_PUSH, 8200L, false);
        assertThat(fcmDelayed.fallbackTriggered()).isTrue();
        assertThat(fcmDelayed.effectiveChannel()).isEqualTo("VOIP_THEN_IVR_CALL");

        var smsTimeout = service.resolveNotificationFallback(OutageChannel.SMS_GATEWAY, 6000L, true);
        assertThat(smsTimeout.fallbackTriggered()).isTrue();
        assertThat(smsTimeout.effectiveChannel()).isEqualTo("WHATSAPP_OR_IVR_OTP");

        var razorpay502 = service.handlePaymentOutage(502, 3);
        assertThat(razorpay502.circuitOpen()).isTrue();
        assertThat(razorpay502.retryAllowed()).isFalse();
        assertThat(razorpay502.suggestedAlternative()).isEqualTo("WALLET_BALANCE");
        assertThat(razorpay502.userSafeMessage()).contains("wallet");
    }

    @Test
    @DisplayName("Enforces non-blocking vs critical force-update gates and parses 6-month-old JSON payloads safely")
    void shouldEnforceVersionGatesAndBackwardCompatibleJsonParsing() {
        var critical = service.evaluateAppVersionPolicy("3.2.1", "3.5.0", "4.2.0");
        assertThat(critical.enforcement()).isEqualTo(UpdateEnforcement.CRITICAL_FORCE_UPDATE);
        assertThat(critical.blocking()).isTrue();
        assertThat(critical.canDismiss()).isFalse();

        var soft = service.evaluateAppVersionPolicy("3.9.0", "3.5.0", "4.2.0");
        assertThat(soft.enforcement()).isEqualTo(UpdateEnforcement.SOFT_UPDATE_RECOMMENDED);
        assertThat(soft.blocking()).isFalse();
        assertThat(soft.canDismiss()).isTrue();

        var parsed = service.parseLegacyCompatiblePayload(Map.of(
                "flat_no", "B-402",
                "resident_id", "88",
                "due_amt", "4500.50"
        ));
        assertThat(parsed.get("flatNumber")).isEqualTo("B-402");
        assertThat(parsed.get("userId")).isEqualTo(88L);
        assertThat((BigDecimal) parsed.get("amount")).isEqualByComparingTo("4500.50");
        assertThat(parsed.get("schemaVersion")).isEqualTo("v1-compat");
    }

    @Test
    @DisplayName("Passes healthy 12-hour guard soak and flags heap leaks, battery thermal throttling, and SQLite WAL risk")
    void shouldValidate12HourSoakStabilityAndDetectLeaksOrSqliteRisk() {
        var healthy12h = service.evaluateSoakHealthSnapshot(new SoakSnapshotInput(
                12.0, 128.0, 152.0, 84.0, 38.4, true, 24, 14500));
        assertThat(healthy12h.healthy()).isTrue();
        assertThat(healthy12h.heapGrowthMbPerHour()).isEqualTo(2.0);
        assertThat(healthy12h.failures()).isEmpty();

        var leakedOverheatCorrupt = service.evaluateSoakHealthSnapshot(new SoakSnapshotInput(
                12.0, 128.0, 320.0, 32.0, 45.2, false, 810, 14500));
        assertThat(leakedOverheatCorrupt.healthy()).isFalse();
        assertThat(leakedOverheatCorrupt.failures()).containsExactlyInAnyOrder(
                "MEMORY_LEAK_DETECTED",
                "BATTERY_THERMAL_EXCEEDED",
                "SQLITE_INTEGRITY_AT_RISK"
        );
    }
}
