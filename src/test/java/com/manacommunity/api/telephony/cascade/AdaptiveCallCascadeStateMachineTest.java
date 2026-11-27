package com.manacommunity.api.telephony.cascade;

import com.manacommunity.api.telephony.cascade.model.CallHealthMetricsDto;
import com.manacommunity.api.telephony.cascade.model.CascadeExecutionResultDto;
import com.manacommunity.api.telephony.cascade.service.AdaptiveCallCascadeStateMachineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveCallCascadeStateMachineTest {

    private AdaptiveCallCascadeStateMachineImpl stateMachine;
    private final Long communityId = 6003L;

    @BeforeEach
    void setUp() {
        stateMachine = new AdaptiveCallCascadeStateMachineImpl();
    }

    @Test
    @DisplayName("Should maintain WebRTC VoIP call when ICE connects in <= 1500ms and packet loss is low")
    void testHealthyWebRtcVoipConnection() {
        CallHealthMetricsDto health = CallHealthMetricsDto.builder()
                .iceConnected(true)
                .iceConnectionDurationMs(850) // Fast 850ms ICE
                .packetLossPercentage(2.5)   // Low loss
                .roundTripTimeMs(80)
                .jitterMs(20)
                .build();

        CascadeExecutionResultDto result = stateMachine.executeCascade(
                "INT-101", communityId, "A-301", "Dunzo", "+919876543210", health, null);

        assertNotNull(result);
        assertEquals(CascadeExecutionResultDto.Channel.WEBRTC_VOIP, result.getFinalChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.APPROVED, result.getApprovalOutcome());
        assertNull(result.getFailoverReason());
    }

    @Test
    @DisplayName("Should failover to GSM PSTN IVR when gate basement packet loss exceeds threshold")
    void testFailoverToPstnOnBasementPacketLoss() {
        // High 25% packet loss in basement gate
        CallHealthMetricsDto health = CallHealthMetricsDto.builder()
                .iceConnected(true)
                .iceConnectionDurationMs(1200)
                .packetLossPercentage(25.0)
                .roundTripTimeMs(350)
                .jitterMs(150)
                .build();

        // Resident presses DTMF '1' (Approve) on the PSTN IVR call
        CascadeExecutionResultDto result = stateMachine.executeCascade(
                "INT-102", communityId, "B-502", "Zomato", "+919876543210", health, "1");

        assertNotNull(result);
        assertEquals(CascadeExecutionResultDto.Channel.GSM_PSTN_IVR, result.getFinalChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.APPROVED, result.getApprovalOutcome());
        assertEquals("1", result.getPstnDtmfReceived());
        assertEquals("HIGH_PACKET_LOSS_BASEMENT_NETWORK", result.getFailoverReason());
    }

    @Test
    @DisplayName("Should record denial when resident presses DTMF '2' on GSM PSTN IVR call")
    void testPstnDenialOnDtmfTwo() {
        CallHealthMetricsDto health = CallHealthMetricsDto.builder()
                .iceConnected(false) // ICE failed completely
                .iceConnectionDurationMs(2000)
                .build();

        CascadeExecutionResultDto result = stateMachine.executeCascade(
                "INT-103", communityId, "C-201", "Unknown Visitor", "+919876543210", health, "2");

        assertNotNull(result);
        assertEquals(CascadeExecutionResultDto.Channel.GSM_PSTN_IVR, result.getFinalChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.DENIED, result.getApprovalOutcome());
        assertEquals("2", result.getPstnDtmfReceived());
    }

    @Test
    @DisplayName("Should cascade to Flash SMS / WhatsApp when WebRTC fails and PSTN call is unanswered")
    void testCascadeToFlashSmsOnPstnNoAnswer() {
        CallHealthMetricsDto health = CallHealthMetricsDto.builder()
                .iceConnected(false)
                .iceConnectionDurationMs(1600) // Timed out
                .build();

        // No DTMF entered (PSTN unanswered or busy)
        CascadeExecutionResultDto result = stateMachine.executeCascade(
                "INT-104", communityId, "D-104", "Courier", "+919876543210", health, null);

        assertNotNull(result);
        assertEquals(CascadeExecutionResultDto.Channel.FLASH_SMS_WHATSAPP, result.getFinalChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.PENDING_RESPONSE, result.getApprovalOutcome());
        assertEquals("PSTN_UNANSWERED_AFTER_WEBRTC_FAILURE", result.getFailoverReason());
    }
}
