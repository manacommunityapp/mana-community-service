package com.manacommunity.api.telephony.intercom.service;

import com.manacommunity.api.telephony.intercom.model.AnswerCallResultDto;
import com.manacommunity.api.telephony.intercom.model.CallSessionDto;
import com.manacommunity.api.telephony.intercom.model.ForkedDeviceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class VoipSignalingOrchestratorImpl implements VoipSignalingOrchestrator {

    private final AtomicLong callCounter = new AtomicLong(9000);
    private final Map<String, CallSessionDto> activeSessions = new ConcurrentHashMap<>();

    @Override
    public CallSessionDto initiateParallelRing(
            String flatIdentifier,
            Long communityId,
            String callerType,
            String callerName,
            String visitorName,
            List<ForkedDeviceDto> targetDevices) {

        String callId = "CALL-" + callCounter.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();

        List<ForkedDeviceDto> forkedList = new ArrayList<>();
        if (targetDevices != null) {
            for (ForkedDeviceDto dev : targetDevices) {
                dev.setRingStatus(ForkedDeviceDto.RingStatus.RINGING);
                dev.setMissedCallBadged(false);
                forkedList.add(dev);
            }
        }

        CallSessionDto session = CallSessionDto.builder()
                .callId(callId)
                .communityId(communityId)
                .flatIdentifier(flatIdentifier)
                .callerType(callerType)
                .callerName(callerName)
                .visitorName(visitorName)
                .initiatedAt(now)
                .state(CallSessionDto.State.RINGING)
                .forkedDevices(forkedList)
                .build();

        activeSessions.put(callId, session);
        log.info("Initiated simultaneous ringing for flat {} across {} devices on call {}",
                flatIdentifier, forkedList.size(), callId);

        return session;
    }

    @Override
    public AnswerCallResultDto answerCall(String callId, String answeringDeviceId) {
        CallSessionDto session = activeSessions.get(callId);
        if (session == null) {
            return AnswerCallResultDto.builder()
                    .success(false)
                    .callId(callId)
                    .answeringDeviceId(answeringDeviceId)
                    .message("Call session not found: " + callId)
                    .build();
        }

        long startNanos = System.nanoTime();

        // Atomic Check-And-Set (CAS) to prevent race conditions when two family members tap accept concurrently
        synchronized (session) {
            if (session.getState() != CallSessionDto.State.RINGING) {
                log.warn("Device {} attempted to answer call {}, but call state is already {}",
                        answeringDeviceId, callId, session.getState());
                return AnswerCallResultDto.builder()
                        .success(false)
                        .callId(callId)
                        .answeringDeviceId(answeringDeviceId)
                        .message("CALL_ALREADY_ANSWERED_BY_ANOTHER_MEMBER")
                        .build();
            }

            session.setState(CallSessionDto.State.ANSWERED);
            session.setAnsweredByDeviceId(answeringDeviceId);
            session.setAnsweredAt(LocalDateTime.now());

            List<String> cancelledDevices = new ArrayList<>();

            // Dispatch immediate cancellation signaling (< 200ms) to other forked devices
            for (ForkedDeviceDto dev : session.getForkedDevices()) {
                if (dev.getDeviceId().equals(answeringDeviceId)) {
                    dev.setRingStatus(ForkedDeviceDto.RingStatus.ANSWERED);
                } else {
                    dev.setRingStatus(ForkedDeviceDto.RingStatus.CANCELLED_ANSWERED_ELSEWHERE);
                    dev.setCancellationReason("CXCallEndedReasonAnsweredElsewhere");
                    // Suppress missed call badge on both iOS CallKit and Android TelecomManager
                    dev.setMissedCallBadged(false);
                    cancelledDevices.add(dev.getDeviceId());
                }
            }

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("Call {} answered by device {}. Dispatched CANCEL to {} devices in {} ms",
                    callId, answeringDeviceId, cancelledDevices.size(), latencyMs);

            return AnswerCallResultDto.builder()
                    .success(true)
                    .callId(callId)
                    .answeringDeviceId(answeringDeviceId)
                    .cancelledDeviceIds(cancelledDevices)
                    .cancellationLatencyMs(latencyMs)
                    .message("CALL_ANSWERED_MEDIA_CONNECTED")
                    .build();
        }
    }

    @Override
    public CallSessionDto terminateCall(String callId, String reason) {
        CallSessionDto session = activeSessions.get(callId);
        if (session != null) {
            synchronized (session) {
                session.setState(CallSessionDto.State.TERMINATED);
                if (session.getForkedDevices() != null) {
                    for (ForkedDeviceDto dev : session.getForkedDevices()) {
                        if (dev.getRingStatus() == ForkedDeviceDto.RingStatus.RINGING) {
                            dev.setRingStatus(ForkedDeviceDto.RingStatus.CANCELLED_ANSWERED_ELSEWHERE);
                            dev.setCancellationReason(reason != null ? reason : "DISMISSED_BY_OTHER_DEVICE");
                            dev.setMissedCallBadged(false);
                        }
                    }
                }
                log.info("Terminated call {} with reason: {}", callId, reason);
            }
        }
        return session;
    }

    @Override
    public CallSessionDto getCallSession(String callId) {
        return activeSessions.get(callId);
    }
}
