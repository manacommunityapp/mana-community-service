package com.manacommunity.api.notification.matrix;

import com.manacommunity.api.telephony.cascade.model.CascadeExecutionResultDto;
import com.manacommunity.api.telephony.intercom.model.ForkedDeviceDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public final class IntercomMatrixDtos {

    private IntercomMatrixDtos() {}

    public enum OemVendor {
        XIAOMI_MIUI,
        VIVO_ORIGINOS,
        OPPO_COLOROS,
        SAMSUNG_ONEUI,
        STOCK_ANDROID,
        APPLE_IOS
    }

    public enum AppLifecycleState {
        FOREGROUND,
        BACKGROUND,
        KILLED_APP,
        ANDROID_DOZE,
        OEM_BATTERY_SAVER
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OemWakeupEvaluationRequest {
        private OemVendor oemVendor;
        private AppLifecycleState appState;
        private boolean fcmHighPriority;
        private int fcmTtlSeconds;
        private boolean apnsVoipPushType;
        private boolean oemAutostartWhitelisted;
        private boolean dozeExempted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OemWakeupEvaluationResult {
        private OemVendor oemVendor;
        private AppLifecycleState appState;
        private boolean payloadCompliant;
        private boolean reliablePushWakeup;
        private String recommendedFallbackChannel;
        private Map<String, String> requiredTransportHeaders;
        private List<String> diagnostics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelMatrixProbeRequest {
        private Long communityId;
        private String flatNumber;
        private String visitorName;
        private String residentPhone;
        private boolean iceConnected;
        private long iceConnectionDurationMs;
        private double packetLossPct;
        private long jitterMs;
        private String pstnDtmf;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelMatrixProbeResult {
        private boolean fcmDozeCompliant;
        private boolean apnsVoipConfigured;
        private CascadeExecutionResultDto.Channel resolvedChannel;
        private CascadeExecutionResultDto.Outcome resolvedOutcome;
        private String failoverReason;
        private List<String> cascadeAuditLog;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamilyResolutionRaceRequest {
        private Long communityId;
        private String flatNumber;
        private String visitorName;
        private List<ForkedDeviceDto> devices;
        private String firstDeviceId;
        private String firstDecision; // APPROVE or DENY
        private String secondDeviceId;
        private String secondDecision; // APPROVE or DENY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamilyResolutionRaceResult {
        private String callId;
        private String winningDeviceId;
        private String winningDecision;
        private boolean secondAttemptRejected;
        private int stillRingingDeviceCount;
        private int missedCallBadgesCreated;
        private long dismissalLatencyMs;
    }
}
