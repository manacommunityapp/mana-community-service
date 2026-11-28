package com.manacommunity.api.sync;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.sync.SyncEnums.*;
import com.manacommunity.api.sync.dto.SyncDtos.*;
import com.manacommunity.api.sync.engine.DeltaSyncEngine;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineSyncServiceImpl implements OfflineSyncService {

    private final SyncChangeLogRepository changeLogRepository;
    private final SyncCheckpointRepository checkpointRepository;
    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final DeltaSyncEngine deltaEngine;

    @Autowired(required = false)
    private GateOfflineDeviceRepository gateDeviceRepository;

    @Autowired(required = false)
    private GateEntryAuditRepository gateAuditRepository;

    @Autowired(required = false)
    private GateOfflineSecurityService gateSecurityService;

    @Override
    @Transactional
    public PushSyncBatchResult processPushSync(PushSyncBatchRequest request) {
        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", request.getUserId()));
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", request.getCommunityId()));

        GateOfflineDevice registeredDevice = gateDeviceRepository != null
                ? gateDeviceRepository.findByCommunityIdAndDeviceId(community.getId(), request.getDeviceId()).orElse(null)
                : null;
        Optional<SyncCheckpoint> existingCheckpoint = checkpointRepository.findByUserIdAndDeviceId(user.getId(), request.getDeviceId());
        long lastCommittedSeq = registeredDevice != null
                ? registeredDevice.getLastSeq()
                : existingCheckpoint.map(SyncCheckpoint::getLastSyncedSeq).orElse(0L);

        List<MutationSyncAck> acks = new ArrayList<>();
        int appliedCount = 0;
        int conflictCount = 0;
        int replayRejectedCount = 0;
        int anomalyCount = 0;
        long latestChangeId = request.getLastKnownChangeId() != null ? request.getLastKnownChangeId() : 0L;
        long highestSeq = lastCommittedSeq;

        if (request.getMutations() != null) {
            for (ClientMutationItem item : request.getMutations()) {
                Optional<SyncChangeLog> existing = changeLogRepository.findByClientMutationId(item.getClientMutationId());
                if (isGateMutation(item) && gateSecurityService != null) {
                    boolean nonceConsumed = item.getPassNonce() != null && gateAuditRepository != null
                            && gateAuditRepository.existsByCommunityIdAndPassNonce(community.getId(), item.getPassNonce());
                    boolean deviceActive = registeredDevice == null || "ACTIVE".equalsIgnoreCase(registeredDevice.getStatus());
                    boolean sigValid = gateSecurityService.verifyDeviceEnvelopeSignature(registeredDevice, item);

                    var gateDecision = deltaEngine.evaluateGateMutation(
                            item, existing, highestSeq, nonceConsumed, sigValid, deviceActive);

                    if (gateDecision.idempotentRepeat()) {
                        acks.add(MutationSyncAck.builder()
                                .clientMutationId(item.getClientMutationId())
                                .serverChangeId(gateDecision.serverChangeId())
                                .entityId(gateDecision.entityId())
                                .status(gateDecision.status())
                                .offlineOverride(gateDecision.offlineOverride())
                                .conflictReason(gateDecision.conflictReason())
                                .message(gateDecision.message())
                                .build());
                        if (gateDecision.status() == SyncStatus.REJECTED_REPLAY) replayRejectedCount++;
                        continue;
                    }

                    if (gateDecision.status() == SyncStatus.REJECTED_REPLAY) {
                        replayRejectedCount++;
                        anomalyCount++;
                        gateSecurityService.recordReplayRejectionAudit(
                                community, user, request.getDeviceId(), item, gateDecision.conflictReason());
                        SyncChangeLog rejectedLog = changeLogRepository.save(SyncChangeLog.builder()
                                .entityType(item.getEntityType())
                                .entityId(item.getEntityId())
                                .community(community)
                                .user(user)
                                .operation(item.getOperation())
                                .payloadJson(item.getPayloadJson())
                                .clientMutationId(item.getClientMutationId())
                                .deviceId(request.getDeviceId())
                                .localSeq(item.getLocalSeq())
                                .passNonce(null)
                                .conflictReason(gateDecision.conflictReason())
                                .offlineOverride(false)
                                .clientTimestamp(item.getClientTimestamp())
                                .serverTimestamp(LocalDateTime.now())
                                .status(SyncStatus.REJECTED_REPLAY)
                                .build());
                        latestChangeId = Math.max(latestChangeId, rejectedLog.getId());
                        acks.add(MutationSyncAck.builder()
                                .clientMutationId(item.getClientMutationId())
                                .serverChangeId(rejectedLog.getId())
                                .entityId(item.getEntityId())
                                .status(SyncStatus.REJECTED_REPLAY)
                                .conflictReason(gateDecision.conflictReason())
                                .anomalyLogged(true)
                                .message(gateDecision.message())
                                .build());
                        continue;
                    }

                    var reconciled = gateSecurityService.reconcileGateMutation(
                            community, user, registeredDevice, request.getDeviceId(), item);
                    SyncChangeLog savedLog = changeLogRepository.save(SyncChangeLog.builder()
                            .entityType(item.getEntityType())
                            .entityId(reconciled.entityId() != null ? reconciled.entityId() : System.currentTimeMillis())
                            .community(community)
                            .user(user)
                            .operation(item.getOperation())
                            .payloadJson(item.getPayloadJson())
                            .clientMutationId(item.getClientMutationId())
                            .deviceId(request.getDeviceId())
                            .localSeq(item.getLocalSeq())
                            .passNonce(item.getPassNonce())
                            .conflictReason(reconciled.conflictReason())
                            .offlineOverride(reconciled.offlineOverride())
                            .clientTimestamp(item.getClientTimestamp())
                            .serverTimestamp(LocalDateTime.now())
                            .status(reconciled.status())
                            .build());

                    latestChangeId = Math.max(latestChangeId, savedLog.getId());
                    if (item.getLocalSeq() != null) {
                        highestSeq = Math.max(highestSeq, item.getLocalSeq());
                    }
                    if (reconciled.status() == SyncStatus.APPLIED) {
                        appliedCount++;
                    } else if (reconciled.status() == SyncStatus.REJECTED_REPLAY) {
                        replayRejectedCount++;
                    } else {
                        conflictCount++;
                    }
                    if (reconciled.anomalyLogged()) anomalyCount++;

                    acks.add(MutationSyncAck.builder()
                            .clientMutationId(item.getClientMutationId())
                            .serverChangeId(savedLog.getId())
                            .entityId(reconciled.entityId())
                            .status(reconciled.status())
                            .offlineOverride(reconciled.offlineOverride())
                            .conflictReason(reconciled.conflictReason())
                            .anomalyLogged(reconciled.anomalyLogged())
                            .message(reconciled.conflictReason() != null ? reconciled.conflictReason() : "Applied & Audited")
                            .build());
                    continue;
                }

                // Generic delta sync fallback (preserves non-gate entity mutations & unit tests)
                var decision = deltaEngine.evaluateMutation(
                        item,
                        existing,
                        Optional.empty(),
                        ConflictResolutionStrategy.SERVER_WINS
                );

                if (decision.status() == SyncStatus.APPLIED) {
                    SyncChangeLog changeLog = SyncChangeLog.builder()
                            .entityType(item.getEntityType())
                            .entityId(item.getEntityId() != null ? item.getEntityId() : System.currentTimeMillis())
                            .community(community)
                            .user(user)
                            .operation(item.getOperation())
                            .payloadJson(item.getPayloadJson())
                            .clientMutationId(item.getClientMutationId())
                            .deviceId(request.getDeviceId())
                            .localSeq(item.getLocalSeq())
                            .passNonce(item.getPassNonce())
                            .clientTimestamp(item.getClientTimestamp())
                            .serverTimestamp(LocalDateTime.now())
                            .status(SyncStatus.APPLIED)
                            .build();

                    SyncChangeLog saved = changeLogRepository.save(changeLog);
                    latestChangeId = Math.max(latestChangeId, saved.getId());
                    if (item.getLocalSeq() != null) {
                        highestSeq = Math.max(highestSeq, item.getLocalSeq());
                    }
                    appliedCount++;

                    acks.add(MutationSyncAck.builder()
                            .clientMutationId(item.getClientMutationId())
                            .serverChangeId(saved.getId())
                            .entityId(saved.getEntityId())
                            .status(SyncStatus.APPLIED)
                            .message("Applied")
                            .build());
                } else {
                    conflictCount++;
                    acks.add(MutationSyncAck.builder()
                            .clientMutationId(item.getClientMutationId())
                            .serverChangeId(decision.serverChangeId())
                            .entityId(item.getEntityId())
                            .status(decision.status())
                            .message(decision.reason())
                            .build());
                }
            }
        }

        updateCheckpoint(user, request.getDeviceId(), latestChangeId, highestSeq, request.getClientAppVersion());

        return PushSyncBatchResult.builder()
                .deviceId(request.getDeviceId())
                .totalProcessed(request.getMutations() != null ? request.getMutations().size() : 0)
                .appliedCount(appliedCount)
                .conflictCount(conflictCount)
                .replayRejectedCount(replayRejectedCount)
                .anomalyCount(anomalyCount)
                .newCheckpointChangeId(latestChangeId)
                .newCheckpointSeq(highestSeq)
                .serverEpochMs(Instant.now().toEpochMilli())
                .acks(acks)
                .build();
    }

    private boolean isGateMutation(ClientMutationItem item) {
        if (item.getEntityType() == SyncEntityType.SCHOOL_BUS_ENTRY
                || item.getEntityType() == SyncEntityType.DIGITAL_PASS) {
            return true;
        }
        return item.getEntityType() == SyncEntityType.VISITOR_PASS
                && (item.getOperation() == SyncOperation.UPDATE
                || item.getEntityId() != null
                || item.getPassCode() != null
                || item.getPassNonce() != null
                || item.getDeviceSignature() != null);
    }

    @Override
    @Transactional(readOnly = true)
    public PullSyncResponse processPullSync(PullSyncRequest request) {
        long since = request.getSinceChangeId() != null ? request.getSinceChangeId() : 0L;
        List<SyncChangeLog> logs = changeLogRepository.findDeltaForCommunity(request.getCommunityId(), since);

        List<SyncChangeItemDto> changeDtos = logs.stream()
                .map(l -> SyncChangeItemDto.builder()
                        .changeId(l.getId())
                        .entityType(l.getEntityType())
                        .entityId(l.getEntityId())
                        .operation(l.getOperation())
                        .payloadJson(l.getPayloadJson())
                        .serverTimestamp(l.getServerTimestamp())
                        .version(l.getVersion())
                        .build())
                .collect(Collectors.toList());

        long latestId = logs.isEmpty() ? since : logs.get(logs.size() - 1).getId();

        return PullSyncResponse.builder()
                .latestChangeId(latestId)
                .hasMore(false)
                .serverEpochMs(Instant.now().toEpochMilli())
                .changes(changeDtos)
                .build();
    }

    @Override
    @Transactional
    public SignedGateRosterBundle registerGateDeviceAndRoster(Long communityId, GateDeviceRegisterRequest request) {
        if (gateSecurityService == null) {
            throw new IllegalStateException("GateOfflineSecurityService not configured");
        }
        return gateSecurityService.registerDeviceAndBuildRoster(communityId, request.getDeviceId(), request.getGateName());
    }

    @Override
    @Transactional(readOnly = true)
    public SignedGateRosterBundle getGateRoster(Long communityId, String deviceId) {
        if (gateSecurityService == null) {
            throw new IllegalStateException("GateOfflineSecurityService not configured");
        }
        return gateSecurityService.getSignedRoster(communityId, deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GateAnomalyDto> getUnresolvedGateAnomalies(Long communityId) {
        if (gateSecurityService == null) return List.of();
        return gateSecurityService.getUnresolvedAnomalies(communityId);
    }

    @Override
    @Transactional
    public GateAnomalyDto resolveGateAnomaly(Long communityId, Long auditId, Long resolverUserId, String note) {
        if (gateSecurityService == null) {
            throw new IllegalStateException("GateOfflineSecurityService not configured");
        }
        return gateSecurityService.resolveAnomaly(communityId, auditId, resolverUserId, note);
    }

    private void updateCheckpoint(AppUser user, String deviceId, Long changeId, Long seqId, String appVersion) {
        SyncCheckpoint ckpt = checkpointRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseGet(() -> SyncCheckpoint.builder()
                        .user(user)
                        .deviceId(deviceId)
                        .build());

        ckpt.setLastSyncedChangeId(Math.max(ckpt.getLastSyncedChangeId(), changeId));
        if (seqId != null && seqId > ckpt.getLastSyncedSeq()) {
            ckpt.setLastSyncedSeq(seqId);
        }
        ckpt.setLastSyncTimestamp(LocalDateTime.now());
        if (appVersion != null) {
            ckpt.setClientAppVersion(appVersion);
        }
        checkpointRepository.save(ckpt);
    }
}
