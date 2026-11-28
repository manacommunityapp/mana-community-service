package com.manacommunity.api.emergency.service;

import com.manacommunity.api.emergency.dto.SosIncidentResponse;
import com.manacommunity.api.emergency.engine.GateLockdownEngine;
import com.manacommunity.api.emergency.engine.SosSlaEngine;
import com.manacommunity.api.emergency.entity.EvacuationBroadcast;
import com.manacommunity.api.emergency.entity.EvacuationMusterRoster;
import com.manacommunity.api.emergency.entity.GateLockdownLog;
import com.manacommunity.api.emergency.entity.GateLockdownLog.LockdownDirective;
import com.manacommunity.api.emergency.entity.SosIncident;
import com.manacommunity.api.emergency.entity.SosIncident.EmergencyType;
import com.manacommunity.api.emergency.entity.SosIncident.IncidentStatus;
import com.manacommunity.api.emergency.entity.SosIncident.Severity;
import com.manacommunity.api.emergency.repository.EvacuationBroadcastRepository;
import com.manacommunity.api.emergency.repository.EvacuationMusterRosterRepository;
import com.manacommunity.api.emergency.repository.GateLockdownLogRepository;
import com.manacommunity.api.emergency.repository.SosIncidentRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Low-Signal (<90B) Lift SOS Ingestion (HTTP + SMS Fallback Deduplication + Audible Guard Siren)
 * & Sub-2s Geo-Targeted Tower Evacuation Roster & Broadcast Engine.
 */
@Service
@RequiredArgsConstructor
public class CompactSosAndEvacuationService {

    private final SosIncidentRepository incidentRepo;
    private final GateLockdownLogRepository lockdownRepo;
    private final EvacuationBroadcastRepository evacuationRepo;
    private final EvacuationMusterRosterRepository rosterRepo;
    private final VisitorPassRepository visitorPassRepo;
    private final CommunityFlatRepository flatRepo;
    private final FlatMembershipRepository membershipRepo;
    private final SosSlaEngine slaEngine;
    private final GateLockdownEngine lockdownEngine;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Real-time O(1) tower presence index backed by Redis-style partition keys: "communityId:TOWER" */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, TowerOccupant>> liveTowerPresence = new ConcurrentHashMap<>();

    @Builder
    public record CompactSosFrame(
            Long communityId,
            Long residentId,
            String towerBlock,
            String locationCode,
            EmergencyType emergencyType,
            long clientEpochSec,
            String idempotencyKey,
            int payloadBytes
    ) {}

    @Builder
    public record CompactSosIngestResult(
            boolean deduplicated,
            String ingestChannel,
            boolean sirenOverride,
            Long nearestGuardId,
            int payloadBytes,
            SosIncidentResponse incident
    ) {}

    @Builder
    public record TowerOccupant(
            String category, // RESIDENT, DOMESTIC_STAFF, VISITOR
            Long personId,
            String name,
            String phone,
            String flatNumber
    ) {}

    @Builder
    public record EvacuationDispatchResult(
            EvacuationBroadcast broadcast,
            long computeTimeMs,
            int totalAlerted,
            List<String> smsFallbackPhones,
            List<EvacuationMusterRoster> roster
    ) {}

    public CompactSosFrame parseCompactFrame(String rawFrame) {
        if (rawFrame == null || rawFrame.isBlank()) {
            throw new IllegalArgumentException("Compact SOS frame is empty");
        }
        String trimmed = rawFrame.trim();
        String[] parts = trimmed.split("\\|", -1);
        if (parts.length < 8 || !"SOS1".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid compact SOS frame format; expected SOS1|comm|res|block|loc|type|ts|idem");
        }
        return CompactSosFrame.builder()
                .communityId(Long.parseLong(parts[1]))
                .residentId(Long.parseLong(parts[2]))
                .towerBlock(parts[3])
                .locationCode(parts[4])
                .emergencyType(decodeType(parts[5]))
                .clientEpochSec(Long.parseLong(parts[6]))
                .idempotencyKey(parts[7])
                .payloadBytes(trimmed.getBytes(StandardCharsets.UTF_8).length)
                .build();
    }

    public static String encodeCompactFrame(
            Long communityId, Long residentId, String towerBlock, String locationCode, String typeCode, long epochSec, String idemKey) {
        return String.join("|",
                "SOS1",
                String.valueOf(communityId),
                String.valueOf(residentId),
                towerBlock != null ? towerBlock : "GEN",
                locationCode != null ? locationCode : "LIFT",
                typeCode != null ? typeCode : "L",
                String.valueOf(epochSec),
                idemKey);
    }

    @Transactional
    public CompactSosIngestResult ingestCompactSos(
            String rawFrame,
            String ingestChannel,
            AppUser resident,
            Community community,
            Long nearestGuardId
    ) {
        CompactSosFrame frame = parseCompactFrame(rawFrame);
        String channel = (ingestChannel != null && !ingestChannel.isBlank()) ? ingestChannel.toUpperCase() : "COMPACT_2G";

        Optional<SosIncident> existing = incidentRepo.findByIdempotencyKey(frame.idempotencyKey());
        if (existing.isPresent()) {
            SosIncident inc = existing.get();
            return CompactSosIngestResult.builder()
                    .deduplicated(true)
                    .ingestChannel(channel)
                    .sirenOverride(inc.isSirenOverride())
                    .nearestGuardId(inc.getNearestGuardId())
                    .payloadBytes(frame.payloadBytes())
                    .incident(SosIncidentResponse.from(inc))
                    .build();
        }

        Severity severity = Severity.CRITICAL;
        int slaSec = slaEngine.getTargetResponseSeconds(severity);
        LockdownDirective directive = lockdownEngine.evaluateDirective(frame.emergencyType());
        boolean lockdown = directive == LockdownDirective.LOCKDOWN_CLOSE_ALL || directive == LockdownDirective.EVACUATION_OPEN_ALL;

        SosIncident incident = incidentRepo.save(SosIncident.builder()
                .community(community)
                .resident(resident)
                .buildingBlock(frame.towerBlock())
                .locationDetails(frame.locationCode())
                .emergencyType(frame.emergencyType())
                .severity(severity)
                .status(IncidentStatus.TRIGGERED)
                .lockdownInitiated(lockdown)
                .slaTargetSeconds(slaSec)
                .idempotencyKey(frame.idempotencyKey())
                .ingestChannel(channel)
                .sirenOverride(true)
                .nearestGuardId(nearestGuardId)
                .triggeredAt(LocalDateTime.now())
                .notes("Compact SOS frame (" + channel + ", " + frame.payloadBytes() + "B) at ts="
                        + Instant.ofEpochSecond(frame.clientEpochSec()))
                .build());

        if (lockdown) {
            lockdownRepo.save(GateLockdownLog.builder()
                    .community(community)
                    .incident(incident)
                    .directive(directive)
                    .reason("Automated directive from compact SOS #" + incident.getId())
                    .initiatedBy(resident)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Critical Guard Tablet Audible Siren Override fanout
        if (messagingTemplate != null) {
            Map<String, Object> sirenFrame = new LinkedHashMap<>();
            sirenFrame.put("incidentId", incident.getId());
            sirenFrame.put("communityId", community.getId());
            sirenFrame.put("emergencyType", frame.emergencyType().name());
            sirenFrame.put("targetTower", frame.towerBlock());
            sirenFrame.put("locationCode", frame.locationCode());
            sirenFrame.put("nearestGuardId", nearestGuardId != null ? nearestGuardId : 0L);
            sirenFrame.put("alarmAudioChannel", "STREAM_ALARM");
            sirenFrame.put("fullScreenIntent", true);
            sirenFrame.put("wakeLockRequired", true);
            sirenFrame.put("loopSirenUntilAck", true);
            sirenFrame.put("ingestChannel", channel);
            try {
                messagingTemplate.convertAndSend("/topic/community/" + community.getId() + "/sos/critical", (Object) sirenFrame);
            } catch (Exception ignored) {
                // Non-fatal in test/disconnected broker mode
            }
        }

        return CompactSosIngestResult.builder()
                .deduplicated(false)
                .ingestChannel(channel)
                .sirenOverride(true)
                .nearestGuardId(nearestGuardId)
                .payloadBytes(frame.payloadBytes())
                .incident(SosIncidentResponse.from(incident))
                .build();
    }

    public void recordTowerPresenceEntry(Long communityId, String towerBlock, TowerOccupant occupant) {
        if (communityId == null || towerBlock == null || occupant == null) return;
        String key = presenceKey(communityId, towerBlock);
        String memberKey = occupant.category() + ":" + (occupant.personId() != null ? occupant.personId() : occupant.phone());
        liveTowerPresence.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(memberKey, occupant);
    }

    public void recordTowerPresenceExit(Long communityId, String towerBlock, String category, Long personId, String phone) {
        if (communityId == null || towerBlock == null) return;
        Map<String, TowerOccupant> map = liveTowerPresence.get(presenceKey(communityId, towerBlock));
        if (map != null) {
            map.remove(category + ":" + (personId != null ? personId : phone));
        }
    }

    @Transactional
    public EvacuationDispatchResult triggerTowerEvacuationBroadcast(
            Long communityId,
            String towerBlock,
            String emergencyType,
            String message,
            String assemblyPoint,
            Long triggeredByUserId
    ) {
        long startNs = System.nanoTime();
        String normTower = towerBlock.trim().toUpperCase();

        Map<String, TowerOccupant> deduplicatedRoster = new LinkedHashMap<>();

        // 1. O(1) real-time presence cache (covers checked-in domestic staff / walk-ins pushed on punch-in)
        Map<String, TowerOccupant> cached = liveTowerPresence.get(presenceKey(communityId, normTower));
        if (cached != null) {
            deduplicatedRoster.putAll(cached);
        }

        // 2. Checked-in visitors inside that specific tower (fast status index scan)
        List<VisitorPass> checkedInVisitors = visitorPassRepo.findByCommunityAndStatuses(
                communityId, List.of(VisitorPass.PassStatus.CHECKED_IN));
        for (VisitorPass vp : checkedInVisitors) {
            if (matchesTower(vp.getFlatNumber(), normTower)) {
                String k = "VISITOR:" + (vp.getVisitorPhone() != null ? vp.getVisitorPhone() : vp.getId());
                deduplicatedRoster.putIfAbsent(k, new TowerOccupant(
                        "VISITOR", vp.getId(), vp.getVisitorName(), vp.getVisitorPhone(), vp.getFlatNumber()));
            }
        }

        // 3. Registered residents in that tower
        List<CommunityFlat> communityFlats = flatRepo.findByCommunityId(communityId);
        for (CommunityFlat flat : communityFlats) {
            if (flat.getTowerBlock() != null && normTower.equalsIgnoreCase(flat.getTowerBlock().trim())) {
                List<FlatMembership> members = membershipRepo.findByFlatIdWithUser(flat.getId());
                for (FlatMembership m : members) {
                    if (!"REVOKED".equalsIgnoreCase(m.getAppAccessStatus()) && m.getUser() != null) {
                        String flatLabel = flat.getTowerBlock() + "-" + flat.getFlatNumber();
                        String k = "RESIDENT:" + m.getUser().getId();
                        deduplicatedRoster.putIfAbsent(k, new TowerOccupant(
                                "RESIDENT", m.getUser().getId(), m.getUser().getFullName(), m.getUser().getPhone(), flatLabel));
                    }
                }
            }
        }

        int residents = 0;
        int staff = 0;
        int visitors = 0;
        List<String> smsFallbackPhones = new ArrayList<>();
        for (TowerOccupant occ : deduplicatedRoster.values()) {
            switch (occ.category()) {
                case "RESIDENT" -> residents++;
                case "DOMESTIC_STAFF" -> {
                    staff++;
                    if (occ.phone() != null && !occ.phone().isBlank()) smsFallbackPhones.add(occ.phone());
                }
                case "VISITOR" -> {
                    visitors++;
                    if (occ.phone() != null && !occ.phone().isBlank()) smsFallbackPhones.add(occ.phone());
                }
            }
        }

        EvacuationBroadcast broadcast = evacuationRepo.save(EvacuationBroadcast.builder()
                .communityId(communityId)
                .towerBlock(normTower)
                .emergencyType(emergencyType != null ? emergencyType : "GAS_LEAK")
                .severity("CRITICAL")
                .message(message)
                .assemblyPoint(assemblyPoint != null ? assemblyPoint : "Main Assembly Lawn")
                .residentsCount(residents)
                .staffCount(staff)
                .visitorsCount(visitors)
                .smsFallbackCount(smsFallbackPhones.size())
                .triggeredBy(triggeredByUserId)
                .build());

        List<EvacuationMusterRoster> savedRoster = new ArrayList<>();
        for (TowerOccupant occ : deduplicatedRoster.values()) {
            savedRoster.add(rosterRepo.save(EvacuationMusterRoster.builder()
                    .broadcastId(broadcast.getId())
                    .communityId(communityId)
                    .towerBlock(normTower)
                    .personCategory(occ.category())
                    .personId(occ.personId())
                    .personName(occ.name() != null ? occ.name() : "Occupant")
                    .contactPhone(occ.phone())
                    .flatNumber(occ.flatNumber())
                    .status("ALERTED")
                    .build()));
        }

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend(
                        "/topic/community/" + communityId + "/evacuation/" + normTower,
                        (Object) Map.of(
                                "broadcastId", broadcast.getId(),
                                "towerBlock", normTower,
                                "emergencyType", broadcast.getEmergencyType(),
                                "message", message,
                                "assemblyPoint", broadcast.getAssemblyPoint(),
                                "totalAlerted", savedRoster.size()
                        ));
            } catch (Exception ignored) {
                // Non-fatal
            }
        }

        long elapsedMs = Math.max(1L, (System.nanoTime() - startNs) / 1_000_000L);
        return EvacuationDispatchResult.builder()
                .broadcast(broadcast)
                .computeTimeMs(elapsedMs)
                .totalAlerted(savedRoster.size())
                .smsFallbackPhones(smsFallbackPhones)
                .roster(savedRoster)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMusterSummary(Long broadcastId) {
        List<EvacuationMusterRoster> entries = rosterRepo.findByBroadcastIdOrderByPersonCategoryAscFlatNumberAsc(broadcastId);
        long safe = entries.stream().filter(e -> "SAFE_AT_ASSEMBLY".equals(e.getStatus())).count();
        return Map.of(
                "broadcastId", broadcastId,
                "totalAlerted", entries.size(),
                "safeCount", safe,
                "unaccountedCount", entries.size() - safe,
                "entries", entries
        );
    }

    @Transactional
    public EvacuationMusterRoster markSafeAtAssembly(Long musterEntryId) {
        EvacuationMusterRoster entry = rosterRepo.findById(musterEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Muster entry not found: " + musterEntryId));
        entry.setStatus("SAFE_AT_ASSEMBLY");
        entry.setMarkedSafeAt(LocalDateTime.now());
        return rosterRepo.save(entry);
    }

    private EmergencyType decodeType(String code) {
        String c = code != null ? code.trim().toUpperCase() : "L";
        return switch (c) {
            case "L", "LIFT_STUCK" -> EmergencyType.LIFT_STUCK;
            case "F", "FIRE" -> EmergencyType.FIRE;
            case "G", "GAS_LEAK" -> EmergencyType.GAS_LEAK;
            case "M", "MEDICAL" -> EmergencyType.MEDICAL;
            case "S", "SECURITY_INTRUDER" -> EmergencyType.SECURITY_INTRUDER;
            case "T", "THEFT" -> EmergencyType.THEFT;
            default -> EmergencyType.GENERAL_PANIC;
        };
    }

    private boolean matchesTower(String flatNumber, String normTower) {
        if (flatNumber == null || normTower == null) return false;
        String f = flatNumber.trim().toUpperCase();
        return f.startsWith(normTower + "-")
                || f.startsWith(normTower + " ")
                || f.contains("TOWER " + normTower)
                || f.startsWith(normTower + "/");
    }

    private String presenceKey(Long communityId, String towerBlock) {
        return communityId + ":" + towerBlock.trim().toUpperCase();
    }
}
