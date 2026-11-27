package com.manacommunity.api.telephony.intercom;

import com.manacommunity.api.telephony.intercom.model.AnswerCallResultDto;
import com.manacommunity.api.telephony.intercom.model.CallSessionDto;
import com.manacommunity.api.telephony.intercom.model.ForkedDeviceDto;
import com.manacommunity.api.telephony.intercom.service.VoipSignalingOrchestratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoipIntercomSignalingTest {

    private VoipSignalingOrchestratorImpl orchestrator;
    private final Long communityId = 6001L;

    @BeforeEach
    void setUp() {
        orchestrator = new VoipSignalingOrchestratorImpl();
    }

    @Test
    @DisplayName("Should fork call to 4 family members and cancel 3 without missed call badges when one answers")
    void testParallelRingingAndZeroMissedCallSpam() {
        ForkedDeviceDto wifeDevice = ForkedDeviceDto.builder()
                .deviceId("DEV-WIFE-01")
                .residentId(101L)
                .residentName("Ananya")
                .platform(ForkedDeviceDto.Platform.IOS)
                .pushToken("token-wife")
                .build();

        ForkedDeviceDto husbandDevice = ForkedDeviceDto.builder()
                .deviceId("DEV-HUSBAND-02")
                .residentId(102L)
                .residentName("Rohan")
                .platform(ForkedDeviceDto.Platform.ANDROID)
                .pushToken("token-husband")
                .build();

        ForkedDeviceDto sonDevice = ForkedDeviceDto.builder()
                .deviceId("DEV-SON-03")
                .residentId(103L)
                .residentName("Aarav")
                .platform(ForkedDeviceDto.Platform.IOS)
                .pushToken("token-son")
                .build();

        ForkedDeviceDto grandPaDevice = ForkedDeviceDto.builder()
                .deviceId("DEV-GRANDPA-04")
                .residentId(104L)
                .residentName("Om Prakash")
                .platform(ForkedDeviceDto.Platform.ANDROID)
                .pushToken("token-grandpa")
                .build();

        List<ForkedDeviceDto> targetDevices = Arrays.asList(wifeDevice, husbandDevice, sonDevice, grandPaDevice);

        CallSessionDto session = orchestrator.initiateParallelRing(
                "A-402", communityId, "GUARD_TABLET", "Guard Somesh", "Swiggy Delivery", targetDevices);

        assertNotNull(session);
        assertEquals(CallSessionDto.State.RINGING, session.getState());
        assertEquals(4, session.getForkedDevices().size());

        // Wife answers on lock screen
        AnswerCallResultDto answerResult = orchestrator.answerCall(session.getCallId(), "DEV-WIFE-01");

        assertNotNull(answerResult);
        assertTrue(answerResult.isSuccess());
        assertEquals("DEV-WIFE-01", answerResult.getAnsweringDeviceId());
        assertEquals(3, answerResult.getCancelledDeviceIds().size());
        assertTrue(answerResult.getCancellationLatencyMs() < 200, "Cancellation must complete within 200ms");

        // Verify session state
        CallSessionDto updatedSession = orchestrator.getCallSession(session.getCallId());
        assertEquals(CallSessionDto.State.ANSWERED, updatedSession.getState());
        assertEquals("DEV-WIFE-01", updatedSession.getAnsweredByDeviceId());

        // Verify that the other 3 devices have suppressed missed call badges
        for (ForkedDeviceDto dev : updatedSession.getForkedDevices()) {
            if ("DEV-WIFE-01".equals(dev.getDeviceId())) {
                assertEquals(ForkedDeviceDto.RingStatus.ANSWERED, dev.getRingStatus());
            } else {
                assertEquals(ForkedDeviceDto.RingStatus.CANCELLED_ANSWERED_ELSEWHERE, dev.getRingStatus());
                assertFalse(dev.isMissedCallBadged(), "Must not trigger missed call badge or recents spam");
                assertEquals("CXCallEndedReasonAnsweredElsewhere", dev.getCancellationReason());
            }
        }
    }

    @Test
    @DisplayName("Should atomically prevent race conditions when two family members tap accept concurrently")
    void testConcurrentAnswerRaceConditionCAS() {
        ForkedDeviceDto dev1 = ForkedDeviceDto.builder().deviceId("DEV-01").build();
        ForkedDeviceDto dev2 = ForkedDeviceDto.builder().deviceId("DEV-02").build();

        CallSessionDto session = orchestrator.initiateParallelRing(
                "B-101", communityId, "VISITOR_KIOSK", "Kiosk 1", "Guest", Arrays.asList(dev1, dev2));

        // First answer wins
        AnswerCallResultDto first = orchestrator.answerCall(session.getCallId(), "DEV-01");
        assertTrue(first.isSuccess());

        // Second answer within 10ms must be rejected
        AnswerCallResultDto second = orchestrator.answerCall(session.getCallId(), "DEV-02");
        assertFalse(second.isSuccess());
        assertEquals("CALL_ALREADY_ANSWERED_BY_ANOTHER_MEMBER", second.getMessage());
    }
}
