package com.manacommunity.api.telephony.cascade.service;

import com.manacommunity.api.telephony.cascade.model.CallHealthMetricsDto;
import com.manacommunity.api.telephony.cascade.model.CascadeExecutionResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AdaptiveCallCascadeStateMachineImpl implements AdaptiveCallCascadeStateMachine {

    private static final long ICE_TIMEOUT_MS = 1500;
    private static final double MAX_TOLERABLE_PACKET_LOSS_PCT = 15.0;
    private static final long MAX_TOLERABLE_JITTER_MS = 120;

    @Override
    public CascadeExecutionResultDto executeCascade(
            String intercomId,
            Long communityId,
            String flatId,
            String visitorName,
            String residentPhone,
            CallHealthMetricsDto webrtcHealth,
            String pstnResponseDtmf) {

        List<String> auditLog = new ArrayList<>();
        auditLog.add("00:00.000 - Triggered intercom for flat " + flatId + " with visitor: " + visitorName);

        // Stage 1: Evaluate WebRTC / VoIP ICE & RTCP Health in first 1.5 seconds
        boolean webrtcHealthy = isWebRtcHealthy(webrtcHealth, auditLog);

        if (webrtcHealthy) {
            auditLog.add("00:01.200 - Stage 1 (WebRTC VoIP): Audio stream connected cleanly with guard tablet.");
            return CascadeExecutionResultDto.builder()
                    .intercomId(intercomId)
                    .communityId(communityId)
                    .finalChannel(CascadeExecutionResultDto.Channel.WEBRTC_VOIP)
                    .approvalOutcome(CascadeExecutionResultDto.Outcome.APPROVED)
                    .webrtcHealth(webrtcHealth)
                    .cascadeAuditLog(auditLog)
                    .build();
        }

        // WebRTC Failed / Degraded -> Immediate Failover to Stage 2: GSM PSTN IVR
        String failoverReason = determineWebRtcFailureReason(webrtcHealth);
        auditLog.add("00:01.550 - Stage 1 Degraded: " + failoverReason + ". Triggering Stage 2 (GSM PSTN IVR) via DID pool.");

        if ("1".equals(pstnResponseDtmf)) {
            auditLog.add("00:04.800 - Stage 2 (GSM PSTN IVR): Call connected to " + residentPhone + ". DTMF '1' (Approve) pressed.");
            return CascadeExecutionResultDto.builder()
                    .intercomId(intercomId)
                    .communityId(communityId)
                    .finalChannel(CascadeExecutionResultDto.Channel.GSM_PSTN_IVR)
                    .approvalOutcome(CascadeExecutionResultDto.Outcome.APPROVED)
                    .webrtcHealth(webrtcHealth)
                    .pstnDtmfReceived("1")
                    .failoverReason(failoverReason)
                    .cascadeAuditLog(auditLog)
                    .build();
        } else if ("2".equals(pstnResponseDtmf)) {
            auditLog.add("00:04.900 - Stage 2 (GSM PSTN IVR): Call connected. DTMF '2' (Deny) pressed.");
            return CascadeExecutionResultDto.builder()
                    .intercomId(intercomId)
                    .communityId(communityId)
                    .finalChannel(CascadeExecutionResultDto.Channel.GSM_PSTN_IVR)
                    .approvalOutcome(CascadeExecutionResultDto.Outcome.DENIED)
                    .webrtcHealth(webrtcHealth)
                    .pstnDtmfReceived("2")
                    .failoverReason(failoverReason)
                    .cascadeAuditLog(auditLog)
                    .build();
        }

        // PSTN Unanswered / Busy / No DTMF -> Failover to Stage 3: Flash SMS / WhatsApp
        auditLog.add("00:15.000 - Stage 2 (GSM PSTN IVR): No answer or line busy. Triggering Stage 3 (Flash SMS / WhatsApp Template).");
        auditLog.add("00:15.300 - Stage 3 Dispatched: High-priority interactive push sent to " + residentPhone);

        return CascadeExecutionResultDto.builder()
                .intercomId(intercomId)
                .communityId(communityId)
                .finalChannel(CascadeExecutionResultDto.Channel.FLASH_SMS_WHATSAPP)
                .approvalOutcome(CascadeExecutionResultDto.Outcome.PENDING_RESPONSE)
                .webrtcHealth(webrtcHealth)
                .failoverReason("PSTN_UNANSWERED_AFTER_WEBRTC_FAILURE")
                .cascadeAuditLog(auditLog)
                .build();
    }

    private boolean isWebRtcHealthy(CallHealthMetricsDto health, List<String> auditLog) {
        if (health == null) {
            auditLog.add("00:00.100 - WebRTC health metrics missing.");
            return false;
        }

        if (!health.isIceConnected()) {
            auditLog.add("00:01.500 - ICE failed to reach connected state.");
            return false;
        }

        if (health.getIceConnectionDurationMs() > ICE_TIMEOUT_MS) {
            auditLog.add("00:01.500 - ICE connection took " + health.getIceConnectionDurationMs() + "ms (threshold: " + ICE_TIMEOUT_MS + "ms).");
            return false;
        }

        if (health.getPacketLossPercentage() > MAX_TOLERABLE_PACKET_LOSS_PCT) {
            auditLog.add("00:01.500 - Severe RTCP packet loss detected: " + health.getPacketLossPercentage() + "% in gate basement.");
            return false;
        }

        if (health.getJitterMs() > MAX_TOLERABLE_JITTER_MS) {
            auditLog.add("00:01.500 - High jitter detected: " + health.getJitterMs() + "ms.");
            return false;
        }

        return true;
    }

    private String determineWebRtcFailureReason(CallHealthMetricsDto health) {
        if (health == null || !health.isIceConnected()) return "ICE_CONNECTION_FAILED";
        if (health.getIceConnectionDurationMs() > ICE_TIMEOUT_MS) return "ICE_CONNECTION_TIMEOUT";
        if (health.getPacketLossPercentage() > MAX_TOLERABLE_PACKET_LOSS_PCT) return "HIGH_PACKET_LOSS_BASEMENT_NETWORK";
        if (health.getJitterMs() > MAX_TOLERABLE_JITTER_MS) return "HIGH_JITTER_UNSTABLE_WIFI";
        return "VOIP_SIGNALING_FAILURE";
    }
}
