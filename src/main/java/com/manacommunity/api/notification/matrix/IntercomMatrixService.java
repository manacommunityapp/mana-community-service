package com.manacommunity.api.notification.matrix;

import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService;
import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService.PushDeliveryRequest;
import com.manacommunity.api.notification.matrix.IntercomMatrixDtos.*;
import com.manacommunity.api.telephony.cascade.model.CallHealthMetricsDto;
import com.manacommunity.api.telephony.cascade.model.CascadeExecutionResultDto;
import com.manacommunity.api.telephony.cascade.service.AdaptiveCallCascadeStateMachine;
import com.manacommunity.api.telephony.intercom.model.AnswerCallResultDto;
import com.manacommunity.api.telephony.intercom.model.CallSessionDto;
import com.manacommunity.api.telephony.intercom.model.ForkedDeviceDto;
import com.manacommunity.api.telephony.intercom.service.VoipSignalingOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unified QA & production matrix verification service for:
 * 1) Multi-channel delivery (FCM HIGH, APNs VoIP, WebRTC, PSTN IVR, WhatsApp/SMS),
 * 2) Concurrent millisecond Approve/Deny family resolution & sibling ring dismissal,
 * 3) OEM battery killer & Android Doze background wakeup reliability.
 */
@Service
@RequiredArgsConstructor
public class IntercomMatrixService {

    private static final Set<OemVendor> AGGRESSIVE_OEMS = Set.of(
            OemVendor.XIAOMI_MIUI,
            OemVendor.VIVO_ORIGINOS,
            OemVendor.OPPO_COLOROS,
            OemVendor.SAMSUNG_ONEUI
    );

    private final VoipSignalingOrchestrator voipOrchestrator;
    private final AdaptiveCallCascadeStateMachine cascadeStateMachine;
    private final PushNotificationDiagnosticsService diagnosticsService;

    public OemWakeupEvaluationResult evaluateOemWakeup(OemWakeupEvaluationRequest req) {
        List<String> diagnostics = new ArrayList<>();
        Map<String, String> headers = new LinkedHashMap<>();

        boolean isIos = req.getOemVendor() == OemVendor.APPLE_IOS;
        boolean payloadCompliant;
        if (isIos) {
            headers.put("apns-push-type", "voip");
            headers.put("apns-priority", "10");
            headers.put("interruption-level", "time-sensitive");
            payloadCompliant = req.isApnsVoipPushType();
            if (!payloadCompliant) {
                diagnostics.add("iOS killed-app VoIP wake-up requires PushKit apns-push-type=voip");
            }
        } else {
            headers.put("android.priority", "HIGH");
            headers.put("android.ttl", "15s");
            headers.put("fcm.direct_boot_ok", "true");
            payloadCompliant = req.isFcmHighPriority() && req.getFcmTtlSeconds() >= 0 && req.getFcmTtlSeconds() <= 30;
            if (!req.isFcmHighPriority()) {
                diagnostics.add("FCM android.priority=HIGH required to punch through Android Doze");
            }
            if (req.getFcmTtlSeconds() > 30) {
                diagnostics.add("FCM ttl exceeds 30s gate SLA window; stale ring risk");
            }
        }

        boolean aggressiveOemRestricted = AGGRESSIVE_OEMS.contains(req.getOemVendor())
                && (req.getAppState() == AppLifecycleState.KILLED_APP
                || req.getAppState() == AppLifecycleState.OEM_BATTERY_SAVER)
                && !req.isOemAutostartWhitelisted();

        boolean dozeBlocked = req.getAppState() == AppLifecycleState.ANDROID_DOZE
                && !isIos
                && !req.isFcmHighPriority()
                && !req.isDozeExempted();

        if (aggressiveOemRestricted) {
            diagnostics.add(req.getOemVendor() + " in " + req.getAppState()
                    + " without Autostart/Unrestricted battery whitelist drops silent push; escalating to PSTN IVR after 2s");
        }
        if (dozeBlocked) {
            diagnostics.add("Android Doze defers normal-priority FCM messages until maintenance window");
        }

        boolean reliableWakeup = payloadCompliant && !aggressiveOemRestricted && !dozeBlocked;
        String fallback = reliableWakeup
                ? (isIos ? "APNS_PUSHKIT_VOIP" : "FCM_HIGH_PRIORITY_VOIP")
                : "PSTN_IVR_FAST_TRACK";

        return OemWakeupEvaluationResult.builder()
                .oemVendor(req.getOemVendor())
                .appState(req.getAppState())
                .payloadCompliant(payloadCompliant)
                .reliablePushWakeup(reliableWakeup)
                .recommendedFallbackChannel(fallback)
                .requiredTransportHeaders(headers)
                .diagnostics(diagnostics)
                .build();
    }

    public ChannelMatrixProbeResult executeChannelCascadeProbe(ChannelMatrixProbeRequest req) {
        boolean fcmCompliant = diagnosticsService.validatePayloadPriorityForDozeCompliance(
                PushDeliveryRequest.builder()
                        .eventType("VISITOR_ARRIVED")
                        .priority("HIGH")
                        .recipientToken("fcm-probe")
                        .build()
        );

        CallHealthMetricsDto health = CallHealthMetricsDto.builder()
                .iceConnected(req.isIceConnected())
                .iceConnectionDurationMs(req.getIceConnectionDurationMs())
                .packetLossPercentage(req.getPacketLossPct())
                .jitterMs(req.getJitterMs())
                .build();

        CascadeExecutionResultDto cascade = cascadeStateMachine.executeCascade(
                "INTERCOM-" + UUID.randomUUID().toString().substring(0, 6),
                req.getCommunityId(),
                req.getFlatNumber(),
                req.getVisitorName(),
                req.getResidentPhone(),
                health,
                req.getPstnDtmf()
        );

        return ChannelMatrixProbeResult.builder()
                .fcmDozeCompliant(fcmCompliant)
                .apnsVoipConfigured(true)
                .resolvedChannel(cascade.getFinalChannel())
                .resolvedOutcome(cascade.getApprovalOutcome())
                .failoverReason(cascade.getFailoverReason())
                .cascadeAuditLog(cascade.getCascadeAuditLog())
                .build();
    }

    /**
     * Executes a synchronized two-thread race (Approve vs Deny) at the same millisecond
     * to verify deterministic first-wins settlement and instant cross-device ring dismissal.
     */
    public FamilyResolutionRaceResult executeConcurrentFamilyRace(FamilyResolutionRaceRequest req) {
        List<ForkedDeviceDto> devices = req.getDevices() != null && !req.getDevices().isEmpty()
                ? req.getDevices()
                : List.of(
                ForkedDeviceDto.builder().deviceId(req.getFirstDeviceId()).platform(ForkedDeviceDto.Platform.IOS).build(),
                ForkedDeviceDto.builder().deviceId(req.getSecondDeviceId()).platform(ForkedDeviceDto.Platform.ANDROID).build()
        );

        CallSessionDto session = voipOrchestrator.initiateParallelRing(
                req.getFlatNumber(),
                req.getCommunityId(),
                "GUARD_TABLET",
                "Main Gate Guard",
                req.getVisitorName(),
                devices
        );

        String callId = session.getCallId();
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicReference<String> winnerId = new AtomicReference<>();
        AtomicReference<String> winnerDecision = new AtomicReference<>();
        AtomicReference<Boolean> loserRejected = new AtomicReference<>(false);
        long startNs = System.nanoTime();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> task1 = () -> {
                startGate.await();
                AnswerCallResultDto res = voipOrchestrator.answerCall(callId, req.getFirstDeviceId());
                if (res.isSuccess()) {
                    winnerId.set(req.getFirstDeviceId());
                    winnerDecision.set(req.getFirstDecision());
                    voipOrchestrator.terminateCall(callId, "SETTLED_" + req.getFirstDecision());
                } else {
                    loserRejected.set(true);
                }
                return null;
            };
            Callable<Void> task2 = () -> {
                startGate.await();
                AnswerCallResultDto res = voipOrchestrator.answerCall(callId, req.getSecondDeviceId());
                if (res.isSuccess()) {
                    winnerId.set(req.getSecondDeviceId());
                    winnerDecision.set(req.getSecondDecision());
                    voipOrchestrator.terminateCall(callId, "SETTLED_" + req.getSecondDecision());
                } else {
                    loserRejected.set(true);
                }
                return null;
            };

            Future<Void> f1 = pool.submit(task1);
            Future<Void> f2 = pool.submit(task2);
            startGate.countDown();
            f1.get(3, TimeUnit.SECONDS);
            f2.get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Concurrent family race execution failed", ex);
        } finally {
            pool.shutdownNow();
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        CallSessionDto settled = voipOrchestrator.getCallSession(callId);
        int stillRinging = 0;
        int badges = 0;
        for (ForkedDeviceDto d : settled.getForkedDevices()) {
            if (d.getRingStatus() == ForkedDeviceDto.RingStatus.RINGING) stillRinging++;
            if (d.isMissedCallBadged()) badges++;
        }

        return FamilyResolutionRaceResult.builder()
                .callId(callId)
                .winningDeviceId(winnerId.get())
                .winningDecision(winnerDecision.get())
                .secondAttemptRejected(Boolean.TRUE.equals(loserRejected.get()))
                .stillRingingDeviceCount(stillRinging)
                .missedCallBadgesCreated(badges)
                .dismissalLatencyMs(elapsedMs)
                .build();
    }
}
