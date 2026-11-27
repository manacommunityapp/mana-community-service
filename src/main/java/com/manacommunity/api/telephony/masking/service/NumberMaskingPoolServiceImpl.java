package com.manacommunity.api.telephony.masking.service;

import com.manacommunity.api.telephony.masking.model.MaskedCallSessionDto;
import com.manacommunity.api.telephony.masking.model.RouteInboundCallResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class NumberMaskingPoolServiceImpl implements NumberMaskingPoolService {

    private final Queue<String> availableDids = new ConcurrentLinkedQueue<>();
    private final Map<String, MaskedCallSessionDto> activeSessionsByDid = new ConcurrentHashMap<>();
    private final Map<String, MaskedCallSessionDto> sessionsById = new ConcurrentHashMap<>();
    private final AtomicLong sessionCounter = new AtomicLong(1000);

    @Override
    public synchronized void initializePool(List<String> virtualDids) {
        availableDids.clear();
        if (virtualDids != null) {
            availableDids.addAll(virtualDids);
        }
        log.info("Initialized DID masking pool with {} virtual numbers", availableDids.size());
    }

    @Override
    public synchronized MaskedCallSessionDto allocateMaskedSession(Long communityId, String partyAPhone, String partyBPhone, int quarantineMinutes) {
        String did = availableDids.poll();
        if (did == null) {
            // Attempt an eager reclaim of expired sessions
            reclaimExpiredSessions();
            did = availableDids.poll();
            if (did == null) {
                throw new IllegalStateException("DID pool exhausted. No virtual numbers currently available.");
            }
        }

        String sessionId = "MSESS-" + sessionCounter.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultExpiry = now.plusMinutes(Math.max(5, quarantineMinutes));

        MaskedCallSessionDto session = MaskedCallSessionDto.builder()
                .sessionId(sessionId)
                .communityId(communityId)
                .partyAPhone(partyAPhone)
                .partyBPhone(partyBPhone)
                .virtualDid(did)
                .state(MaskedCallSessionDto.State.ALLOCATED)
                .allocatedAt(now)
                .coolingExpiryTime(defaultExpiry)
                .build();

        sessionsById.put(sessionId, session);
        activeSessionsByDid.put(did, session);
        log.info("Allocated DID {} for session {} between {} and {}", did, sessionId, partyAPhone, partyBPhone);

        return session;
    }

    @Override
    public MaskedCallSessionDto recordCallEnd(String sessionId) {
        MaskedCallSessionDto session = sessionsById.get(sessionId);
        if (session != null) {
            synchronized (session) {
                session.setState(MaskedCallSessionDto.State.COOLING_DOWN);
                session.setCallEndedAt(LocalDateTime.now());
                log.info("Call ended for session {}. DID {} entered cooling-down quarantine until {}",
                        sessionId, session.getVirtualDid(), session.getCoolingExpiryTime());
            }
        }
        return session;
    }

    @Override
    public RouteInboundCallResultDto routeInboundCall(String callingPhone, String calledDid) {
        MaskedCallSessionDto session = activeSessionsByDid.get(calledDid);

        if (session == null) {
            return RouteInboundCallResultDto.builder()
                    .success(false)
                    .inboundCallingPhone(callingPhone)
                    .inboundCalledDid(calledDid)
                    .status(RouteInboundCallResultDto.RoutingStatus.REJECTED_UNKNOWN_CALLER)
                    .ivrMessage("No active session mapped to this virtual number.")
                    .build();
        }

        synchronized (session) {
            LocalDateTime now = LocalDateTime.now();

            // Check if quarantine expired
            if (session.getState() == MaskedCallSessionDto.State.COOLING_DOWN && now.isAfter(session.getCoolingExpiryTime())) {
                session.setState(MaskedCallSessionDto.State.EXPIRED);
                return RouteInboundCallResultDto.builder()
                        .success(false)
                        .inboundCallingPhone(callingPhone)
                        .inboundCalledDid(calledDid)
                        .status(RouteInboundCallResultDto.RoutingStatus.REJECTED_EXPIRED_IVR)
                        .ivrMessage("This delivery intercom session has expired. Please contact the security desk.")
                        .build();
            }

            // Route between Party A and Party B
            String destinationPhone = null;
            if (callingPhone.equals(session.getPartyAPhone())) {
                destinationPhone = session.getPartyBPhone();
            } else if (callingPhone.equals(session.getPartyBPhone())) {
                destinationPhone = session.getPartyAPhone();
            }

            if (destinationPhone == null) {
                return RouteInboundCallResultDto.builder()
                        .success(false)
                        .inboundCallingPhone(callingPhone)
                        .inboundCalledDid(calledDid)
                        .status(RouteInboundCallResultDto.RoutingStatus.REJECTED_UNKNOWN_CALLER)
                        .ivrMessage("Unauthorized caller phone.")
                        .build();
            }

            RouteInboundCallResultDto.RoutingStatus status;
            if (session.getState() == MaskedCallSessionDto.State.COOLING_DOWN) {
                session.setLateCallbackCount(session.getLateCallbackCount() + 1);
                status = RouteInboundCallResultDto.RoutingStatus.ROUTED_QUARANTINE_CALLBACK;
                log.info("Handled late callback #{} in quarantine window on DID {} to {}",
                        session.getLateCallbackCount(), calledDid, destinationPhone);
            } else {
                session.setState(MaskedCallSessionDto.State.IN_CALL);
                status = RouteInboundCallResultDto.RoutingStatus.ROUTED_ACTIVE_SESSION;
            }

            return RouteInboundCallResultDto.builder()
                    .success(true)
                    .inboundCallingPhone(callingPhone)
                    .inboundCalledDid(calledDid)
                    .routeDestinationPhone(destinationPhone)
                    .status(status)
                    .build();
        }
    }

    @Override
    public synchronized int reclaimExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int reclaimed = 0;

        for (Iterator<Map.Entry<String, MaskedCallSessionDto>> it = activeSessionsByDid.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, MaskedCallSessionDto> entry = it.next();
            MaskedCallSessionDto session = entry.getValue();

            boolean expired = session.getState() == MaskedCallSessionDto.State.EXPIRED ||
                    (session.getState() == MaskedCallSessionDto.State.COOLING_DOWN && now.isAfter(session.getCoolingExpiryTime()));

            if (expired) {
                session.setState(MaskedCallSessionDto.State.RECLAIMED);
                it.remove();
                availableDids.offer(session.getVirtualDid());
                reclaimed++;
                log.info("Safely reclaimed DID {} from expired session {}", session.getVirtualDid(), session.getSessionId());
            }
        }
        return reclaimed;
    }

    @Override
    public int getAvailableDidCount() {
        return availableDids.size();
    }

    @Override
    public MaskedCallSessionDto getSession(String sessionId) {
        return sessionsById.get(sessionId);
    }
}
