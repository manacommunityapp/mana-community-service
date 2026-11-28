package com.manacommunity.api.whatsapp.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * High-concurrency state machine resolving race conditions between
 * WhatsApp Cloud API webhook quick-replies and Mygate Mobile App actions.
 * Enforces atomic Compare-And-Swap (CAS) transitions with winner/loser notification.
 */
@Service
@Slf4j
public class GateApprovalStateMachine {

    public enum EntryStatus { PENDING, APPROVED, DENIED, EXPIRED }
    public enum ApprovalChannel { WHATSAPP_QUICK_REPLY, MOBILE_APP, GUARD_TABLET, IVR_GSM }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitorEntrySession {
        private String entryId;
        private String societyId;
        private String flatNumber;
        private String visitorName;
        private String visitorPhone;
        private String purpose;

        @Builder.Default
        private EntryStatus status = EntryStatus.PENDING;
        private String resolvedByUser;
        private ApprovalChannel resolvedViaChannel;
        private Instant resolvedAt;
        private int version;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApprovalResolutionResult {
        private String entryId;
        private boolean accepted;
        private EntryStatus finalStatus;
        private String winnerUser;
        private ApprovalChannel winnerChannel;
        private Instant resolvedAt;
        private boolean superseded;
        private String residentNotification;
        private boolean triggerGateBarrierOpen;
    }

    private final Map<String, AtomicReference<VisitorEntrySession>> activeEntries = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("hh:mm:ss a")
            .withZone(ZoneId.of("Asia/Kolkata"));

    /**
     * Registers a new visitor entry at the gate awaiting resident decision.
     */
    public void registerVisitorEntry(VisitorEntrySession entry) {
        activeEntries.put(entry.getEntryId(), new AtomicReference<>(entry));
        log.info("Registered visitor entry [{}] for flat [{}] - Visitor: [{}]",
                entry.getEntryId(), entry.getFlatNumber(), entry.getVisitorName());
    }

    public VisitorEntrySession getEntry(String entryId) {
        AtomicReference<VisitorEntrySession> ref = activeEntries.get(entryId);
        return ref != null ? ref.get() : null;
    }

    /**
     * Executes atomic Compare-And-Swap (CAS) state transition.
     * Guarantees that only the first action (whether WhatsApp button or Mobile App tap) wins the race.
     */
    public ApprovalResolutionResult submitDecision(
            String entryId,
            String targetDecision, // "APPROVE" or "DENY"
            String actingUser,
            ApprovalChannel channel) {

        AtomicReference<VisitorEntrySession> ref = activeEntries.get(entryId);
        if (ref == null) {
            return ApprovalResolutionResult.builder()
                    .entryId(entryId)
                    .accepted(false)
                    .finalStatus(EntryStatus.EXPIRED)
                    .superseded(true)
                    .residentNotification("Error: Visitor entry not found or expired from active gate cache.")
                    .triggerGateBarrierOpen(false)
                    .build();
        }

        EntryStatus intendedStatus = "APPROVE".equalsIgnoreCase(targetDecision) ?
                EntryStatus.APPROVED : EntryStatus.DENIED;

        while (true) {
            VisitorEntrySession current = ref.get();

            // Check if already resolved by another channel
            if (current.getStatus() != EntryStatus.PENDING) {
                String timeStr = current.getResolvedAt() != null ?
                        TIME_FORMATTER.format(current.getResolvedAt()) : "earlier";

                String notification = String.format(
                        "Action superseded: Entry for %s was already %s by co-resident [%s] at %s via %s.",
                        current.getVisitorName(),
                        current.getStatus(),
                        current.getResolvedByUser(),
                        timeStr,
                        formatChannelName(current.getResolvedViaChannel())
                );

                log.warn("Race condition detected for entry [{}]. Channel [{}] attempted [{}] but entry was already [{}] by [{}] via [{}]",
                        entryId, channel, targetDecision, current.getStatus(),
                        current.getResolvedByUser(), current.getResolvedViaChannel());

                return ApprovalResolutionResult.builder()
                        .entryId(entryId)
                        .accepted(false)
                        .finalStatus(current.getStatus())
                        .winnerUser(current.getResolvedByUser())
                        .winnerChannel(current.getResolvedViaChannel())
                        .resolvedAt(current.getResolvedAt())
                        .superseded(true)
                        .residentNotification(notification)
                        .triggerGateBarrierOpen(false)
                        .build();
            }

            // Attempt atomic transition
            VisitorEntrySession updated = VisitorEntrySession.builder()
                    .entryId(current.getEntryId())
                    .societyId(current.getSocietyId())
                    .flatNumber(current.getFlatNumber())
                    .visitorName(current.getVisitorName())
                    .visitorPhone(current.getVisitorPhone())
                    .purpose(current.getPurpose())
                    .status(intendedStatus)
                    .resolvedByUser(actingUser)
                    .resolvedViaChannel(channel)
                    .resolvedAt(Instant.now())
                    .version(current.getVersion() + 1)
                    .build();

            if (ref.compareAndSet(current, updated)) {
                log.info("Entry [{}] successfully transitioned to [{}] by [{}] via [{}]",
                        entryId, intendedStatus, actingUser, channel);

                boolean openBarrier = (intendedStatus == EntryStatus.APPROVED);

                String notification = intendedStatus == EntryStatus.APPROVED ?
                        String.format("Entry APPROVED for %s. Boom barrier opening.", current.getVisitorName()) :
                        String.format("Entry DENIED for %s. Guard notified.", current.getVisitorName());

                return ApprovalResolutionResult.builder()
                        .entryId(entryId)
                        .accepted(true)
                        .finalStatus(intendedStatus)
                        .winnerUser(actingUser)
                        .winnerChannel(channel)
                        .resolvedAt(updated.getResolvedAt())
                        .superseded(false)
                        .residentNotification(notification)
                        .triggerGateBarrierOpen(openBarrier)
                        .build();
            }
            // CAS failed due to concurrent modification, loop again
        }
    }

    private String formatChannelName(ApprovalChannel channel) {
        if (channel == null) return "Unknown";
        return switch (channel) {
            case WHATSAPP_QUICK_REPLY -> "WhatsApp";
            case MOBILE_APP -> "Mygate Mobile App";
            case GUARD_TABLET -> "Security Guard Tablet";
            case IVR_GSM -> "Automated IVR Phone Call";
        };
    }

    public void clear() {
        activeEntries.clear();
    }
}
