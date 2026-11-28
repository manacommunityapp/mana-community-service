package com.manacommunity.api.notification.matrix;

import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService;
import com.manacommunity.api.notification.matrix.IntercomMatrixDtos.*;
import com.manacommunity.api.telephony.cascade.model.CascadeExecutionResultDto;
import com.manacommunity.api.telephony.cascade.service.AdaptiveCallCascadeStateMachineImpl;
import com.manacommunity.api.telephony.intercom.model.ForkedDeviceDto;
import com.manacommunity.api.telephony.intercom.service.VoipSignalingOrchestratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntercomNotificationMatrixTest {

    private IntercomMatrixService matrixService;

    @BeforeEach
    void setUp() {
        matrixService = new IntercomMatrixService(
                new VoipSignalingOrchestratorImpl(),
                new AdaptiveCallCascadeStateMachineImpl(),
                new PushNotificationDiagnosticsService()
        );
    }

    @Test
    void verifiesMultiChannelCascadeFcmApnsWebRtcPstnAndWhatsAppFallback() {
        // Stage 1 healthy WebRTC VoIP
        ChannelMatrixProbeResult webrtcOk = matrixService.executeChannelCascadeProbe(
                ChannelMatrixProbeRequest.builder()
                        .communityId(1L).flatNumber("A-402").visitorName("Ramesh").residentPhone("9876543210")
                        .iceConnected(true).iceConnectionDurationMs(850).packetLossPct(1.2).jitterMs(20)
                        .build()
        );
        assertTrue(webrtcOk.isFcmDozeCompliant());
        assertTrue(webrtcOk.isApnsVoipConfigured());
        assertEquals(CascadeExecutionResultDto.Channel.WEBRTC_VOIP, webrtcOk.getResolvedChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.APPROVED, webrtcOk.getResolvedOutcome());

        // Basement packet loss -> Stage 2 PSTN IVR Press 1 to Approve
        ChannelMatrixProbeResult pstnApprove = matrixService.executeChannelCascadeProbe(
                ChannelMatrixProbeRequest.builder()
                        .communityId(1L).flatNumber("A-402").visitorName("Blinkit").residentPhone("9876543210")
                        .iceConnected(true).iceConnectionDurationMs(1200).packetLossPct(24.0).jitterMs(50)
                        .pstnDtmf("1")
                        .build()
        );
        assertEquals(CascadeExecutionResultDto.Channel.GSM_PSTN_IVR, pstnApprove.getResolvedChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.APPROVED, pstnApprove.getResolvedOutcome());

        // Press 2 to Deny
        ChannelMatrixProbeResult pstnDeny = matrixService.executeChannelCascadeProbe(
                ChannelMatrixProbeRequest.builder()
                        .communityId(1L).flatNumber("A-402").visitorName("Unknown").residentPhone("9876543210")
                        .iceConnected(false).iceConnectionDurationMs(1800)
                        .pstnDtmf("2")
                        .build()
        );
        assertEquals(CascadeExecutionResultDto.Channel.GSM_PSTN_IVR, pstnDeny.getResolvedChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.DENIED, pstnDeny.getResolvedOutcome());

        // Unanswered PSTN -> Stage 3 WhatsApp / SMS Flash
        ChannelMatrixProbeResult waFallback = matrixService.executeChannelCascadeProbe(
                ChannelMatrixProbeRequest.builder()
                        .communityId(1L).flatNumber("A-402").visitorName("Swiggy").residentPhone("9876543210")
                        .iceConnected(false).iceConnectionDurationMs(2100)
                        .pstnDtmf(null)
                        .build()
        );
        assertEquals(CascadeExecutionResultDto.Channel.FLASH_SMS_WHATSAPP, waFallback.getResolvedChannel());
        assertEquals(CascadeExecutionResultDto.Outcome.PENDING_RESPONSE, waFallback.getResolvedOutcome());
    }

    @Test
    void concurrentMillisecondApproveVsDenyWinsDeterministicallyAndDismissesAllDevices() {
        List<ForkedDeviceDto> familyDevices = List.of(
                ForkedDeviceDto.builder().deviceId("DEV-WIFE-IOS").residentId(101L).platform(ForkedDeviceDto.Platform.IOS).build(),
                ForkedDeviceDto.builder().deviceId("DEV-HUSBAND-XIAOMI").residentId(102L).platform(ForkedDeviceDto.Platform.ANDROID).build(),
                ForkedDeviceDto.builder().deviceId("DEV-PARENT-SAMSUNG").residentId(103L).platform(ForkedDeviceDto.Platform.ANDROID).build(),
                ForkedDeviceDto.builder().deviceId("DEV-TEEN-VIVO").residentId(104L).platform(ForkedDeviceDto.Platform.ANDROID).build()
        );

        FamilyResolutionRaceResult race = matrixService.executeConcurrentFamilyRace(
                FamilyResolutionRaceRequest.builder()
                        .communityId(1L)
                        .flatNumber("B-902")
                        .visitorName("Urban Company Plumber")
                        .devices(familyDevices)
                        .firstDeviceId("DEV-WIFE-IOS")
                        .firstDecision("APPROVE")
                        .secondDeviceId("DEV-HUSBAND-XIAOMI")
                        .secondDecision("DENY")
                        .build()
        );

        assertNotNull(race.getWinningDeviceId());
        assertTrue(List.of("DEV-WIFE-IOS", "DEV-HUSBAND-XIAOMI").contains(race.getWinningDeviceId()));
        assertTrue(race.isSecondAttemptRejected());
        assertEquals(0, race.getStillRingingDeviceCount());
        assertEquals(0, race.getMissedCallBadgesCreated());
        assertTrue(race.getDismissalLatencyMs() < 1000);
    }

    @ParameterizedTest
    @EnumSource(value = OemVendor.class, names = {"XIAOMI_MIUI", "VIVO_ORIGINOS", "OPPO_COLOROS", "SAMSUNG_ONEUI"})
    void escalatesAggressiveOemKilledOrBatterySaverDevicesToPstnIvrWithoutAutostart(OemVendor oem) {
        OemWakeupEvaluationResult unwhitelisted = matrixService.evaluateOemWakeup(
                OemWakeupEvaluationRequest.builder()
                        .oemVendor(oem)
                        .appState(AppLifecycleState.KILLED_APP)
                        .fcmHighPriority(true)
                        .fcmTtlSeconds(15)
                        .oemAutostartWhitelisted(false)
                        .dozeExempted(false)
                        .build()
        );
        assertFalse(unwhitelisted.isReliablePushWakeup());
        assertEquals("PSTN_IVR_FAST_TRACK", unwhitelisted.getRecommendedFallbackChannel());

        OemWakeupEvaluationResult whitelisted = matrixService.evaluateOemWakeup(
                OemWakeupEvaluationRequest.builder()
                        .oemVendor(oem)
                        .appState(AppLifecycleState.KILLED_APP)
                        .fcmHighPriority(true)
                        .fcmTtlSeconds(15)
                        .oemAutostartWhitelisted(true)
                        .dozeExempted(true)
                        .build()
        );
        assertTrue(whitelisted.isReliablePushWakeup());
        assertEquals("FCM_HIGH_PRIORITY_VOIP", whitelisted.getRecommendedFallbackChannel());
    }

    @Test
    void verifiesDozeAndIosVoipPushKitRequirements() {
        OemWakeupEvaluationResult normalDoze = matrixService.evaluateOemWakeup(
                OemWakeupEvaluationRequest.builder()
                        .oemVendor(OemVendor.STOCK_ANDROID)
                        .appState(AppLifecycleState.ANDROID_DOZE)
                        .fcmHighPriority(false)
                        .fcmTtlSeconds(15)
                        .build()
        );
        assertFalse(normalDoze.isPayloadCompliant());
        assertFalse(normalDoze.isReliablePushWakeup());

        OemWakeupEvaluationResult iosVoip = matrixService.evaluateOemWakeup(
                OemWakeupEvaluationRequest.builder()
                        .oemVendor(OemVendor.APPLE_IOS)
                        .appState(AppLifecycleState.KILLED_APP)
                        .apnsVoipPushType(true)
                        .build()
        );
        assertTrue(iosVoip.isPayloadCompliant());
        assertTrue(iosVoip.isReliablePushWakeup());
        assertEquals("APNS_PUSHKIT_VOIP", iosVoip.getRecommendedFallbackChannel());
    }
}
