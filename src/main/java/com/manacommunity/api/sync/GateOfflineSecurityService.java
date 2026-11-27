package com.manacommunity.api.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.communityops.pass.DigitalPass;
import com.manacommunity.api.communityops.pass.DigitalPassRepository;
import com.manacommunity.api.communityops.pass.PassStatus;
import com.manacommunity.api.communityops.pass.PassType;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.schoolbus.entity.BusTripLog;
import com.manacommunity.api.schoolbus.entity.RegisteredChild;
import com.manacommunity.api.schoolbus.repository.BusTripLogRepository;
import com.manacommunity.api.schoolbus.repository.RegisteredChildRepository;
import com.manacommunity.api.sync.SyncEnums.SyncEntityType;
import com.manacommunity.api.sync.SyncEnums.SyncStatus;
import com.manacommunity.api.sync.dto.SyncDtos.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.visitor.entity.VisitorAuditLog;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorAuditLogRepository;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles offline gate tablet enrollment, signed pre-authorized morning-rush roster generation,
 * HMAC payload/signature verification, pass-nonce anti-replay checks, and domain reconciliation
 * (VisitorPass, DigitalPass, SchoolBus BusTripLog) with append-only GateEntryAudit records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GateOfflineSecurityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GateOfflineDeviceRepository deviceRepository;
    private final GateEntryAuditRepository auditRepository;
    private final CommunityRepository communityRepository;
    private final VisitorPassRepository visitorPassRepository;
    private final VisitorAuditLogRepository visitorAuditLogRepository;
    private final DigitalPassRepository digitalPassRepository;
    private final RegisteredChildRepository registeredChildRepository;
    private final BusTripLogRepository busTripLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SignedGateRosterBundle registerDeviceAndBuildRoster(
            Long communityId, String deviceId, String gateName) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        GateOfflineDevice device = deviceRepository.findByCommunityIdAndDeviceId(communityId, deviceId)
                .orElseGet(() -> {
                    byte[] secretBytes = new byte[32];
                    SECURE_RANDOM.nextBytes(secretBytes);
                    return GateOfflineDevice.builder()
                            .community(community)
                            .deviceId(deviceId)
                            .gateName(gateName)
                            .signingSecret(HexFormat.of().formatHex(secretBytes))
                            .status("ACTIVE")
                            .lastSeq(0L)
                            .rosterVersion(1L)
                            .build();
                });

        device.setGateName(gateName);
        device.setLastSeenAt(LocalDateTime.now());
        device.setRosterVersion(device.getRosterVersion() + 1);
        device = deviceRepository.save(device);

        return buildSignedRosterBundle(communityId, device);
    }

    @Transactional(readOnly = true)
    public SignedGateRosterBundle getSignedRoster(Long communityId, String deviceId) {
        GateOfflineDevice device = deviceRepository.findByCommunityIdAndDeviceId(communityId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("GateOfflineDevice", deviceId));
        return buildSignedRosterBundle(communityId, device);
    }

    private SignedGateRosterBundle buildSignedRosterBundle(Long communityId, GateOfflineDevice device) {
        Instant now = Instant.now();
        List<SignedPreAuthEntryDto> entries = new ArrayList<>();
        List<String> revokedCodes = new ArrayList<>();

        // 1. Pre-authorized / currently inside visitors
        List<VisitorPass> passes = visitorPassRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        for (VisitorPass vp : passes) {
            if (vp.getStatus() == VisitorPass.PassStatus.APPROVED || vp.getStatus() == VisitorPass.PassStatus.CHECKED_IN) {
                entries.add(SignedPreAuthEntryDto.builder()
                        .entityType(SyncEntityType.VISITOR_PASS)
                        .entityId(vp.getId())
                        .passCode(vp.getPassCode())
                        .subjectName(vp.getVisitorName())
                        .flatOrStop(vp.getFlatNumber())
                        .vehicleOrBusRef(vp.getVehicleNumber())
                        .status(vp.getStatus().name())
                        .validFrom(vp.getCreatedAt() != null ? vp.getCreatedAt().toString() : null)
                        .validUntil(vp.getExpectedAt() != null ? vp.getExpectedAt().plusHours(12).toString() : null)
                        .singleUse(true)
                        .build());
            } else if (vp.getStatus() == VisitorPass.PassStatus.REJECTED || vp.getStatus() == VisitorPass.PassStatus.CANCELLED) {
                if (vp.getPassCode() != null) revokedCodes.add(vp.getPassCode());
            }
        }

        // 2. Active Digital Passes
        for (DigitalPass dp : digitalPassRepository.findByCommunityIdAndStatusAndValidUntilAfter(
                communityId, PassStatus.ACTIVE, now)) {
            entries.add(SignedPreAuthEntryDto.builder()
                    .entityType(SyncEntityType.DIGITAL_PASS)
                    .entityId(dp.getId())
                    .passCode(dp.getToken())
                    .subjectName(dp.getVisitorName())
                    .flatOrStop(dp.getFlatNumber())
                    .vehicleOrBusRef(dp.getVehicleNumber())
                    .status(dp.getStatus().name())
                    .validFrom(dp.getValidFrom().toString())
                    .validUntil(dp.getValidUntil().toString())
                    .singleUse(dp.getPassType() == PassType.DELIVERY || dp.getPassType() == PassType.GUEST)
                    .build());
        }

        // 3. Pre-authorized Morning School-Bus Children
        for (RegisteredChild child : registeredChildRepository.findByCommunityIdAndStatus(communityId, "ACTIVE")) {
            entries.add(SignedPreAuthEntryDto.builder()
                    .entityType(SyncEntityType.SCHOOL_BUS_ENTRY)
                    .entityId(child.getId())
                    .passCode("SB-" + child.getId() + "-" + (child.getBusId() != null ? child.getBusId() : "0"))
                    .subjectName(child.getName())
                    .flatOrStop(child.getStopName())
                    .vehicleOrBusRef(child.getBusId() != null ? "BUS-" + child.getBusId() : child.getSchoolName())
                    .status(child.getStatus())
                    .validFrom(LocalDate.now().atStartOfDay().toString())
                    .validUntil(LocalDate.now().plusDays(1).atStartOfDay().toString())
                    .singleUse(false)
                    .build());
        }

        long serverEpochMs = now.toEpochMilli();
        long expiresAtEpochMs = serverEpochMs + 4 * 60 * 60 * 1000L; // 4-hour morning/evening rush window
        String canonical = communityId + "|" + device.getDeviceId() + "|" + device.getRosterVersion()
                + "|" + serverEpochMs + "|" + entries.size() + "|" + revokedCodes.size();
        String sig = hmacSha256(device.getSigningSecret(), canonical);

        return SignedGateRosterBundle.builder()
                .communityId(communityId)
                .deviceId(device.getDeviceId())
                .gateName(device.getGateName())
                .signingSecret(device.getSigningSecret())
                .rosterVersion(device.getRosterVersion())
                .serverEpochMs(serverEpochMs)
                .expiresAtEpochMs(expiresAtEpochMs)
                .bundleSignature(sig)
                .entries(entries)
                .revokedPassCodes(revokedCodes)
                .build();
    }

    public boolean verifyDeviceEnvelopeSignature(GateOfflineDevice device, ClientMutationItem item) {
        if (device == null || item.getDeviceSignature() == null || item.getDeviceSignature().isBlank()) {
            return device == null; // allow legacy clients without registered device entry
        }
        String canonical = device.getDeviceId() + "|" + item.getClientMutationId() + "|"
                + (item.getLocalSeq() != null ? item.getLocalSeq() : 0L) + "|"
                + item.getEntityType() + "|"
                + (item.getPassCode() != null ? item.getPassCode() : "") + "|"
                + (item.getPassNonce() != null ? item.getPassNonce() : "");
        String expected = hmacSha256(device.getSigningSecret(), canonical);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                item.getDeviceSignature().trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Transactional
    public ReconciliationOutcome reconcileGateMutation(
            Community community,
            AppUser guardUser,
            GateOfflineDevice device,
            String deviceId,
            ClientMutationItem item) {

        LocalDateTime occurredAt = item.getClientTimestamp() != null ? item.getClientTimestamp() : LocalDateTime.now();
        String gateName = item.getGateName() != null ? item.getGateName() : (device != null ? device.getGateName() : "Main Gate");
        String passCode = item.getPassCode() != null ? item.getPassCode() : extractField(item.getPayloadJson(), "passCode");
        SyncStatus status = SyncStatus.APPLIED;
        boolean offlineOverride = false;
        String conflictReason = null;
        Long resolvedEntityId = item.getEntityId();

        if (item.getEntityType() == SyncEntityType.VISITOR_PASS) {
            Optional<VisitorPass> passOpt = resolveVisitorPass(item.getEntityId(), passCode);
            if (passOpt.isEmpty()) {
                status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                offlineOverride = true;
                conflictReason = "VISITOR_PASS_NOT_FOUND";
            } else {
                VisitorPass pass = passOpt.get();
                resolvedEntityId = pass.getId();
                if (pass.getCommunity() != null && !Objects.equals(pass.getCommunity().getId(), community.getId())) {
                    status = SyncStatus.REJECTED_REPLAY;
                    conflictReason = "CROSS_COMMUNITY_PASS_REJECTED";
                } else {
                    String action = extractField(item.getPayloadJson(), "action");
                    boolean isCheckOut = "CHECK_OUT".equalsIgnoreCase(action) || "VISITOR_CHECK_OUT".equalsIgnoreCase(action);
                    if (isCheckOut) {
                        if (pass.getStatus() == VisitorPass.PassStatus.CHECKED_IN) {
                            pass.setStatus(VisitorPass.PassStatus.CHECKED_OUT);
                            pass.setCheckedOutAt(occurredAt);
                            pass.setGateOut(gateName);
                            pass.setGuardOut(guardUser != null ? guardUser.getFullName() : deviceId);
                            visitorPassRepository.save(pass);
                        } else if (pass.getStatus() == VisitorPass.PassStatus.CHECKED_OUT) {
                            status = SyncStatus.CONFLICT_SERVER_WINS;
                            conflictReason = "ALREADY_CHECKED_OUT";
                        } else {
                            status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                            offlineOverride = true;
                            conflictReason = "CHECKOUT_CONFLICT_STATUS_" + pass.getStatus();
                        }
                    } else {
                        if (pass.getStatus() == VisitorPass.PassStatus.APPROVED) {
                            pass.setStatus(VisitorPass.PassStatus.CHECKED_IN);
                            pass.setCheckedInAt(occurredAt);
                            pass.setGateIn(gateName);
                            pass.setGuardIn(guardUser != null ? guardUser.getFullName() : deviceId);
                            visitorPassRepository.save(pass);
                        } else if (pass.getStatus() == VisitorPass.PassStatus.CHECKED_OUT
                                && pass.getCheckedOutAt() != null && pass.getCheckedOutAt().isAfter(occurredAt)) {
                            // Physical exit already recorded after offline entry: do NOT revert status to CHECKED_IN
                            status = SyncStatus.CONFLICT_SERVER_WINS;
                            offlineOverride = true;
                            conflictReason = "ENTRY_OUT_OF_ORDER_EXIT_ALREADY_RECORDED";
                        } else if (pass.getStatus() == VisitorPass.PassStatus.CHECKED_IN) {
                            status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                            offlineOverride = true;
                            conflictReason = "CONCURRENT_DUPLICATE_GATE_CHECKIN";
                        } else if (pass.getStatus() == VisitorPass.PassStatus.REJECTED
                                || pass.getStatus() == VisitorPass.PassStatus.CANCELLED
                                || pass.getStatus() == VisitorPass.PassStatus.EXPIRED) {
                            status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                            offlineOverride = true;
                            conflictReason = "PASS_" + pass.getStatus() + "_WHILE_OFFLINE";
                        }
                    }

                    visitorAuditLogRepository.save(VisitorAuditLog.builder()
                            .visitorPassId(pass.getId())
                            .visitorName(pass.getVisitorName())
                            .action("OFFLINE_SYNC_" + status.name())
                            .performedBy(guardUser != null ? guardUser.getFullName() : deviceId)
                            .details(conflictReason != null ? conflictReason : "Reconciled offline gate approval at " + gateName)
                            .timestamp(LocalDateTime.now())
                            .build());
                }
            }
        } else if (item.getEntityType() == SyncEntityType.SCHOOL_BUS_ENTRY) {
            Long childId = resolvedEntityId != null ? resolvedEntityId : parseLongField(item.getPayloadJson(), "childId");
            if (childId == null) {
                status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                offlineOverride = true;
                conflictReason = "MISSING_SCHOOL_BUS_CHILD_ID";
            } else {
                Optional<RegisteredChild> childOpt = registeredChildRepository.findById(childId);
                if (childOpt.isEmpty()) {
                    status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                    offlineOverride = true;
                    conflictReason = "SCHOOL_BUS_CHILD_NOT_FOUND";
                } else {
                    RegisteredChild child = childOpt.get();
                    resolvedEntityId = child.getId();
                    if (!"ACTIVE".equalsIgnoreCase(child.getStatus())) {
                        status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                        offlineOverride = true;
                        conflictReason = "CHILD_DEACTIVATED_WHILE_OFFLINE";
                    }
                    LocalDate tripDate = occurredAt.toLocalDate();
                    String direction = extractField(item.getPayloadJson(), "direction");
                    boolean isDrop = "DROP".equalsIgnoreCase(direction) || "RETURN".equalsIgnoreCase(direction);
                    BusTripLog tripLog = busTripLogRepository.findByChildIdAndTripDate(child.getId(), tripDate)
                            .orElseGet(() -> BusTripLog.builder()
                                    .childId(child.getId())
                                    .tripDate(tripDate)
                                    .busId(child.getBusId())
                                    .attendance("PRESENT")
                                    .build());

                    if (!isDrop && tripLog.getPickupTime() != null) {
                        status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                        offlineOverride = true;
                        conflictReason = conflictReason != null ? conflictReason : "DUPLICATE_MORNING_BUS_BOARDING";
                    } else if (isDrop && tripLog.getDropTime() != null) {
                        status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                        offlineOverride = true;
                        conflictReason = conflictReason != null ? conflictReason : "DUPLICATE_RETURN_BUS_DROP";
                    } else {
                        if (isDrop) tripLog.setDropTime(occurredAt);
                        else tripLog.setPickupTime(occurredAt);
                        if (guardUser != null && tripLog.getDriverName() == null) {
                            tripLog.setDriverName("Gate: " + gateName);
                        }
                        busTripLogRepository.save(tripLog);
                    }
                }
            }
        } else if (item.getEntityType() == SyncEntityType.DIGITAL_PASS) {
            Optional<DigitalPass> dpOpt = item.getEntityId() != null
                    ? digitalPassRepository.findById(item.getEntityId())
                    : (passCode != null ? digitalPassRepository.findByToken(passCode) : Optional.empty());
            if (dpOpt.isEmpty()) {
                status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                offlineOverride = true;
                conflictReason = "DIGITAL_PASS_NOT_FOUND";
            } else {
                DigitalPass dp = dpOpt.get();
                resolvedEntityId = dp.getId();
                if (dp.getStatus() == PassStatus.REVOKED || dp.getStatus() == PassStatus.EXPIRED) {
                    status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                    offlineOverride = true;
                    conflictReason = "DIGITAL_PASS_" + dp.getStatus() + "_WHILE_OFFLINE";
                } else if (dp.getStatus() == PassStatus.USED
                        && (dp.getPassType() == PassType.DELIVERY || dp.getPassType() == PassType.GUEST)) {
                    status = SyncStatus.CONFLICT_ANOMALY_LOGGED;
                    offlineOverride = true;
                    conflictReason = "SINGLE_USE_DIGITAL_PASS_ALREADY_USED";
                } else if (dp.getPassType() == PassType.DELIVERY || dp.getPassType() == PassType.GUEST) {
                    dp.setStatus(PassStatus.USED);
                    dp.setUsedAt(Instant.now());
                    dp.setUsedByGuard(guardUser != null ? guardUser.getId() : null);
                    digitalPassRepository.save(dp);
                }
            }
        }

        // Persist immutable GateEntryAudit fact unless already recorded for this mutation
        if (auditRepository.findByDeviceIdAndClientMutationId(deviceId, item.getClientMutationId()).isEmpty()) {
            auditRepository.save(GateEntryAudit.builder()
                    .community(community)
                    .deviceId(deviceId)
                    .localSeq(item.getLocalSeq())
                    .clientMutationId(item.getClientMutationId())
                    .entityType(item.getEntityType())
                    .entityId(resolvedEntityId)
                    .passCode(passCode)
                    .passNonce(item.getPassNonce())
                    .gateName(gateName)
                    .guardUserId(guardUser != null ? guardUser.getId() : null)
                    .occurredAt(occurredAt)
                    .reconciledAt(LocalDateTime.now())
                    .syncStatus(status)
                    .offlineOverride(offlineOverride)
                    .conflictReason(conflictReason)
                    .deviceSignature(item.getDeviceSignature())
                    .resolved(status == SyncStatus.APPLIED)
                    .build());
        }

        if (device != null && item.getLocalSeq() != null && item.getLocalSeq() > device.getLastSeq()) {
            device.setLastSeq(item.getLocalSeq());
            device.setLastSeenAt(LocalDateTime.now());
            deviceRepository.save(device);
        }

        return new ReconciliationOutcome(
                status,
                resolvedEntityId,
                offlineOverride,
                conflictReason,
                status != SyncStatus.APPLIED
        );
    }

    @Transactional
    public void recordReplayRejectionAudit(
            Community community, AppUser guardUser, String deviceId, ClientMutationItem item, String reason) {
        if (auditRepository.findByDeviceIdAndClientMutationId(deviceId, item.getClientMutationId()).isPresent()) {
            return;
        }
        String passCode = item.getPassCode() != null ? item.getPassCode() : extractField(item.getPayloadJson(), "passCode");
        auditRepository.save(GateEntryAudit.builder()
                .community(community)
                .deviceId(deviceId)
                .localSeq(item.getLocalSeq())
                .clientMutationId(item.getClientMutationId())
                .entityType(item.getEntityType())
                .entityId(item.getEntityId())
                .passCode(passCode)
                .passNonce(null) // keep original consumed nonce intact; do not violate unique nonce index
                .gateName(item.getGateName())
                .guardUserId(guardUser != null ? guardUser.getId() : null)
                .occurredAt(item.getClientTimestamp() != null ? item.getClientTimestamp() : LocalDateTime.now())
                .reconciledAt(LocalDateTime.now())
                .syncStatus(SyncStatus.REJECTED_REPLAY)
                .offlineOverride(false)
                .conflictReason(reason)
                .deviceSignature(item.getDeviceSignature())
                .resolved(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<GateAnomalyDto> getUnresolvedAnomalies(Long communityId) {
        return auditRepository.findByCommunityIdAndResolvedFalseAndSyncStatusInOrderByReconciledAtDesc(
                communityId,
                List.of(SyncStatus.CONFLICT_ANOMALY_LOGGED, SyncStatus.CONFLICT_SERVER_WINS, SyncStatus.REJECTED_REPLAY)
        ).stream().map(this::toAnomalyDto).toList();
    }

    @Transactional
    public GateAnomalyDto resolveAnomaly(Long communityId, Long auditId, Long resolverUserId, String note) {
        GateEntryAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new ResourceNotFoundException("GateEntryAudit", auditId));
        if (!Objects.equals(audit.getCommunity().getId(), communityId)) {
            throw new IllegalArgumentException("Anomaly does not belong to community " + communityId);
        }
        audit.setResolved(true);
        audit.setResolvedBy(resolverUserId);
        audit.setResolvedAt(LocalDateTime.now());
        audit.setResolutionNote(note);
        return toAnomalyDto(auditRepository.save(audit));
    }

    private GateAnomalyDto toAnomalyDto(GateEntryAudit a) {
        return GateAnomalyDto.builder()
                .id(a.getId())
                .deviceId(a.getDeviceId())
                .localSeq(a.getLocalSeq())
                .clientMutationId(a.getClientMutationId())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .passCode(a.getPassCode())
                .passNonce(a.getPassNonce())
                .gateName(a.getGateName())
                .occurredAt(a.getOccurredAt())
                .reconciledAt(a.getReconciledAt())
                .syncStatus(a.getSyncStatus())
                .offlineOverride(a.isOfflineOverride())
                .conflictReason(a.getConflictReason())
                .resolved(a.isResolved())
                .resolutionNote(a.getResolutionNote())
                .build();
    }

    private Optional<VisitorPass> resolveVisitorPass(Long entityId, String passCode) {
        if (entityId != null) {
            Optional<VisitorPass> byId = visitorPassRepository.findById(entityId);
            if (byId.isPresent()) return byId;
        }
        if (passCode != null && !passCode.isBlank()) {
            return visitorPassRepository.findByPassCode(passCode.trim());
        }
        return Optional.empty();
    }

    private String extractField(String json, String field) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.hasNonNull(field) ? node.get(field).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLongField(String json, String field) {
        String val = extractField(json, field);
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    public record ReconciliationOutcome(
            SyncStatus status,
            Long entityId,
            boolean offlineOverride,
            String conflictReason,
            boolean anomalyLogged) {}
}
