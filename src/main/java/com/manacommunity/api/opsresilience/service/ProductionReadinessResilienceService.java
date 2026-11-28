package com.manacommunity.api.opsresilience.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Production-readiness resilience checks covering third-party outage degradation,
 * semantic app force-update / backward compatibility, and 12-hour guard soak health.
 */
@Service
public class ProductionReadinessResilienceService {

    public static final long FCM_DEGRADE_THRESHOLD_MS = 5000L;
    public static final double MAX_HEAP_GROWTH_MB_PER_HOUR = 8.0;
    public static final double MAX_SAFE_BATTERY_TEMP_C = 43.0;
    public static final int MAX_UNFLUSHED_SQLITE_WAL_FRAMES = 500;

    public enum OutageChannel {
        FCM_PUSH,
        SMS_GATEWAY,
        RAZORPAY_UPI
    }

    public enum UpdateEnforcement {
        UP_TO_DATE,
        SOFT_UPDATE_RECOMMENDED,
        CRITICAL_FORCE_UPDATE
    }

    public record NotificationFallbackResult(
            boolean delivered,
            String primaryChannel,
            String effectiveChannel,
            boolean fallbackTriggered,
            String userVisibleMessage
    ) {}

    public record PaymentOutageDecision(
            boolean retryAllowed,
            boolean circuitOpen,
            String suggestedAlternative,
            String userSafeMessage
    ) {}

    public record AppVersionDecision(
            UpdateEnforcement enforcement,
            boolean promptShown,
            boolean blocking,
            boolean canDismiss,
            String minCriticalVersion,
            String recommendedVersion,
            String clientVersion
    ) {}

    public record SoakSnapshotInput(
            double durationHours,
            double startHeapMb,
            double endHeapMb,
            double gcReclaimedPct,
            double batteryTempCelsius,
            boolean sqliteIntegrityOk,
            int unflushedWalFrames,
            int qrScansProcessed
    ) {}

    public record SoakHealthVerdict(
            boolean healthy,
            double heapGrowthMbPerHour,
            List<String> failures
    ) {}

    /**
     * Evaluates third-party notification outage / latency and cascades cleanly to VoIP/IVR or WhatsApp.
     */
    public NotificationFallbackResult resolveNotificationFallback(OutageChannel channel, long observedLatencyMs, boolean providerError) {
        return switch (channel) {
            case FCM_PUSH -> {
                if (providerError || observedLatencyMs > FCM_DEGRADE_THRESHOLD_MS) {
                    yield new NotificationFallbackResult(true, "FCM_PUSH", "VOIP_THEN_IVR_CALL", true,
                            "Push delayed; escalated visitor ring via VoIP & automated IVR call");
                }
                yield new NotificationFallbackResult(true, "FCM_PUSH", "FCM_PUSH", false, "Delivered via push");
            }
            case SMS_GATEWAY -> {
                if (providerError || observedLatencyMs > FCM_DEGRADE_THRESHOLD_MS) {
                    yield new NotificationFallbackResult(true, "SMS_GATEWAY", "WHATSAPP_OR_IVR_OTP", true,
                            "SMS gateway timed out; dispatched fallback OTP via WhatsApp/voice IVR");
                }
                yield new NotificationFallbackResult(true, "SMS_GATEWAY", "SMS_GATEWAY", false, "Delivered via SMS");
            }
            case RAZORPAY_UPI -> new NotificationFallbackResult(false, "RAZORPAY_UPI", "NONE", false,
                    "Use handlePaymentOutage for payment gateway degradation");
        };
    }

    public PaymentOutageDecision handlePaymentOutage(int httpStatusCode, int consecutiveFailures) {
        boolean isGatewayFault = httpStatusCode == 502 || httpStatusCode == 503 || httpStatusCode == 504;
        boolean circuitOpen = isGatewayFault && consecutiveFailures >= 3;
        if (circuitOpen) {
            return new PaymentOutageDecision(false, true, "WALLET_BALANCE",
                    "Bank/UPI gateway is temporarily degraded (502/504). Pay via society wallet or retry shortly.");
        }
        return new PaymentOutageDecision(true, false, "UPI_INTENT_RETRY",
                isGatewayFault ? "Temporary payment hiccup detected; safe idempotent retry available."
                        : "Payment gateway healthy.");
    }

    public AppVersionDecision evaluateAppVersionPolicy(String clientVersion, String minCriticalVersion, String recommendedVersion) {
        if (compareSemVer(clientVersion, minCriticalVersion) < 0) {
            return new AppVersionDecision(UpdateEnforcement.CRITICAL_FORCE_UPDATE,
                    true, true, false, minCriticalVersion, recommendedVersion, clientVersion);
        }
        if (compareSemVer(clientVersion, recommendedVersion) < 0) {
            return new AppVersionDecision(UpdateEnforcement.SOFT_UPDATE_RECOMMENDED,
                    true, false, true, minCriticalVersion, recommendedVersion, clientVersion);
        }
        return new AppVersionDecision(UpdateEnforcement.UP_TO_DATE,
                false, false, true, minCriticalVersion, recommendedVersion, clientVersion);
    }

    /**
     * Upcasts 6-month-old legacy mobile client payloads without failing on renamed or missing v2 fields.
     */
    public Map<String, Object> parseLegacyCompatiblePayload(Map<String, Object> legacyPayload) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Object flatNo = legacyPayload.getOrDefault("flatNumber", legacyPayload.get("flat_no"));
        Object userId = legacyPayload.getOrDefault("userId", legacyPayload.get("resident_id"));
        Object rawAmount = legacyPayload.getOrDefault("amount", legacyPayload.getOrDefault("due_amt", "0"));

        normalized.put("flatNumber", flatNo != null ? String.valueOf(flatNo) : "");
        normalized.put("userId", userId != null ? Long.parseLong(String.valueOf(userId)) : 0L);
        normalized.put("amount", new BigDecimal(String.valueOf(rawAmount)));
        normalized.put("schemaVersion", legacyPayload.getOrDefault("schemaVersion", "v1-compat"));
        return normalized;
    }

    public SoakHealthVerdict evaluateSoakHealthSnapshot(SoakSnapshotInput snapshot) {
        List<String> failures = new ArrayList<>();
        double duration = Math.max(1.0, snapshot.durationHours());
        double heapGrowthPerHr = (snapshot.endHeapMb() - snapshot.startHeapMb()) / duration;

        if (heapGrowthPerHr > MAX_HEAP_GROWTH_MB_PER_HOUR && snapshot.gcReclaimedPct() < 60.0) {
            failures.add("MEMORY_LEAK_DETECTED");
        }
        if (snapshot.batteryTempCelsius() > MAX_SAFE_BATTERY_TEMP_C) {
            failures.add("BATTERY_THERMAL_EXCEEDED");
        }
        if (!snapshot.sqliteIntegrityOk() || snapshot.unflushedWalFrames() > MAX_UNFLUSHED_SQLITE_WAL_FRAMES) {
            failures.add("SQLITE_INTEGRITY_AT_RISK");
        }
        return new SoakHealthVerdict(failures.isEmpty(), Math.round(heapGrowthPerHr * 100.0) / 100.0, List.copyOf(failures));
    }

    public static int compareSemVer(String left, String right) {
        int[] a = parseSemVer(left);
        int[] b = parseSemVer(right);
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return 0;
    }

    private static int[] parseSemVer(String version) {
        if (version == null || version.isBlank()) {
            return new int[]{0, 0, 0};
        }
        String clean = version.trim().replaceFirst("^[vV]", "").split("-", 2)[0];
        String[] parts = clean.split("\\.");
        int[] out = new int[3];
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            out[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", "0"));
        }
        return out;
    }
}
