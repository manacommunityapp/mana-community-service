package com.manacommunity.api.governance.service;

import com.manacommunity.api.governance.entity.AgmAttendanceLedger;
import com.manacommunity.api.governance.entity.GovernanceMeeting;
import com.manacommunity.api.governance.entity.GovernanceResolution;
import com.manacommunity.api.governance.repository.AgmAttendanceLedgerRepository;
import com.manacommunity.api.governance.repository.GovernanceMeetingRepository;
import com.manacommunity.api.governance.repository.GovernanceResolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-time hybrid AGM (In-Person QR + Online WebSocket) attendance, proxy transfer/revocation,
 * and resolution voting quorum gate.
 */
@Service
@RequiredArgsConstructor
public class AgmQuorumService {

    private final GovernanceMeetingRepository meetingRepo;
    private final GovernanceResolutionRepository resolutionRepo;
    private final AgmAttendanceLedgerRepository ledgerRepo;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public AgmAttendanceLedger registerProxy(Long meetingId, Long flatId, Long ownerUserId, Long proxyHolderUserId) {
        GovernanceMeeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));

        long holderCount = ledgerRepo.countByMeetingIdAndProxyHolderUserIdAndStatusIn(
                meetingId, proxyHolderUserId, List.of("REGISTERED_PROXY", "CHECKED_IN"));
        if (holderCount >= meeting.getMaxProxiesPerHolder()) {
            throw new IllegalStateException("Proxy holder already holds maximum allowed proxies ("
                    + meeting.getMaxProxiesPerHolder() + ")");
        }

        AgmAttendanceLedger entry = ledgerRepo.findByMeetingIdAndFlatId(meetingId, flatId)
                .orElseGet(() -> AgmAttendanceLedger.builder()
                        .meetingId(meetingId)
                        .communityId(meeting.getCommunityId())
                        .flatId(flatId)
                        .build());

        if (entry.getId() != null && "CHECKED_IN".equals(entry.getStatus()) && !"PROXY".equals(entry.getAttendeeRole())) {
            throw new IllegalStateException("Owner already checked in; proxy cannot override active owner presence");
        }

        entry.setAttendeeUserId(proxyHolderUserId);
        entry.setProxyHolderUserId(proxyHolderUserId);
        entry.setAttendeeRole("PROXY");
        entry.setAttendanceMode("REGISTERED_PROXY");
        entry.setStatus("REGISTERED_PROXY");
        entry.setRevokedAt(null);
        return ledgerRepo.save(entry);
    }

    @Transactional
    public Map<String, Object> checkIn(Long meetingId, Long flatId, Long userId, String attendeeRole, String attendanceMode) {
        GovernanceMeeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));

        String normalizedRole = (attendeeRole != null && !attendeeRole.isBlank()) ? attendeeRole.toUpperCase() : "OWNER";
        String normalizedMode = (attendanceMode != null && !attendanceMode.isBlank()) ? attendanceMode.toUpperCase() : "IN_PERSON_QR";

        AgmAttendanceLedger entry = ledgerRepo.findByMeetingIdAndFlatId(meetingId, flatId)
                .orElseGet(() -> AgmAttendanceLedger.builder()
                        .meetingId(meetingId)
                        .communityId(meeting.getCommunityId())
                        .flatId(flatId)
                        .build());

        boolean proxySuperseded = false;
        if ("PROXY".equalsIgnoreCase(normalizedRole)) {
            if (entry.getId() != null && "CHECKED_IN".equals(entry.getStatus()) && !"PROXY".equals(entry.getAttendeeRole())) {
                throw new IllegalStateException("Owner is already checked in for this flat; proxy entry denied");
            }
            if (entry.getProxyHolderUserId() != null && !entry.getProxyHolderUserId().equals(userId)) {
                throw new IllegalStateException("User is not the registered proxy holder for this flat");
            }
            entry.setProxyHolderUserId(userId);
        } else {
            // Owner / Co-owner takes over the flat's single AGM seat -> auto-revoke any active proxy without double-counting
            if ("PROXY".equals(entry.getAttendeeRole()) && ("CHECKED_IN".equals(entry.getStatus()) || "REGISTERED_PROXY".equals(entry.getStatus()))) {
                proxySuperseded = true;
                entry.setRevokedAt(OffsetDateTime.now());
            }
            entry.setProxyHolderUserId(null);
        }

        entry.setAttendeeUserId(userId);
        entry.setAttendeeRole(normalizedRole);
        entry.setAttendanceMode(normalizedMode);
        entry.setStatus("CHECKED_IN");
        ledgerRepo.save(entry);

        Map<String, Object> status = recomputeAndBroadcastQuorum(meeting);
        Map<String, Object> response = new LinkedHashMap<>(status);
        response.put("flatId", flatId);
        response.put("attendeeRole", normalizedRole);
        response.put("attendanceMode", normalizedMode);
        response.put("proxySuperseded", proxySuperseded);
        return response;
    }

    @Transactional
    public Map<String, Object> revokeProxy(Long meetingId, Long flatId, Long ownerUserId) {
        GovernanceMeeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));

        AgmAttendanceLedger entry = ledgerRepo.findByMeetingIdAndFlatId(meetingId, flatId)
                .orElseThrow(() -> new IllegalArgumentException("No proxy or attendance entry found for flat " + flatId));

        if (!"PROXY".equals(entry.getAttendeeRole())) {
            throw new IllegalStateException("Active seat is held directly by owner/co-owner, not a proxy");
        }
        entry.setStatus("PROXY_REVOKED");
        entry.setRevokedAt(OffsetDateTime.now());
        ledgerRepo.save(entry);

        Map<String, Object> status = recomputeAndBroadcastQuorum(meeting);
        Map<String, Object> response = new LinkedHashMap<>(status);
        response.put("flatId", flatId);
        response.put("proxyRevoked", true);
        return response;
    }

    @Transactional
    public GovernanceResolution openResolutionVoting(Long resolutionId, Long meetingId) {
        GovernanceMeeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));
        GovernanceResolution resolution = resolutionRepo.findById(resolutionId)
                .orElseThrow(() -> new IllegalArgumentException("Resolution not found: " + resolutionId));

        recomputeAndBroadcastQuorum(meeting);
        if (!meeting.isQuorumMet()) {
            double currentPct = computePct(meeting.getQuorumConfirmed(), meeting.getTotalEligibleUnits());
            throw new IllegalStateException(String.format(
                    "AGM quorum not met (%.2f%% present, requires %.2f%%)",
                    currentPct,
                    meeting.getRequiredQuorumPct() != null ? meeting.getRequiredQuorumPct().doubleValue() : 25.0));
        }

        resolution.setMeetingId(meetingId);
        resolution.setVotingStatus("VOTING_OPEN");
        return resolutionRepo.save(resolution);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLiveQuorumStatus(Long meetingId) {
        GovernanceMeeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));
        return toQuorumPayload(meeting);
    }

    private Map<String, Object> recomputeAndBroadcastQuorum(GovernanceMeeting meeting) {
        int checkedIn = (int) ledgerRepo.countByMeetingIdAndStatus(meeting.getId(), "CHECKED_IN");
        double pct = computePct(checkedIn, meeting.getTotalEligibleUnits());
        double requiredPct = meeting.getRequiredQuorumPct() != null ? meeting.getRequiredQuorumPct().doubleValue() : 25.0;
        boolean met = pct >= requiredPct;

        meeting.setQuorumConfirmed(checkedIn);
        meeting.setQuorumMet(met);
        meetingRepo.save(meeting);

        Map<String, Object> payload = toQuorumPayload(meeting);
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/agm/" + meeting.getId() + "/quorum", payload);
            } catch (Exception ignored) {
                // Non-fatal if WS broker is unavailable
            }
        }
        return payload;
    }

    private Map<String, Object> toQuorumPayload(GovernanceMeeting meeting) {
        double pct = computePct(meeting.getQuorumConfirmed(), meeting.getTotalEligibleUnits());
        return Map.of(
                "meetingId", meeting.getId(),
                "quorumConfirmed", meeting.getQuorumConfirmed(),
                "totalEligibleUnits", meeting.getTotalEligibleUnits(),
                "requiredQuorumPct", meeting.getRequiredQuorumPct() != null ? meeting.getRequiredQuorumPct().doubleValue() : 25.0,
                "quorumPercentage", pct,
                "quorumMet", meeting.isQuorumMet()
        );
    }

    private double computePct(int checkedIn, int totalEligible) {
        if (totalEligible <= 0) return 0.0;
        return BigDecimal.valueOf(checkedIn * 100.0 / totalEligible)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
