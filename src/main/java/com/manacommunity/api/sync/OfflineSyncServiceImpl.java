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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public PushSyncBatchResult processPushSync(PushSyncBatchRequest request) {
        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", request.getUserId()));
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", request.getCommunityId()));

        List<MutationSyncAck> acks = new ArrayList<>();
        int appliedCount = 0;
        int conflictCount = 0;
        long latestChangeId = request.getLastKnownChangeId() != null ? request.getLastKnownChangeId() : 0L;

        if (request.getMutations() != null) {
            for (ClientMutationItem item : request.getMutations()) {
                Optional<SyncChangeLog> existing = changeLogRepository.findByClientMutationId(item.getClientMutationId());

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
                            .clientTimestamp(item.getClientTimestamp())
                            .serverTimestamp(LocalDateTime.now())
                            .status(SyncStatus.APPLIED)
                            .build();

                    SyncChangeLog saved = changeLogRepository.save(changeLog);
                    latestChangeId = Math.max(latestChangeId, saved.getId());
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

        // Update checkpoint
        updateCheckpoint(user, request.getDeviceId(), latestChangeId, request.getClientAppVersion());

        return PushSyncBatchResult.builder()
                .deviceId(request.getDeviceId())
                .totalProcessed(request.getMutations() != null ? request.getMutations().size() : 0)
                .appliedCount(appliedCount)
                .conflictCount(conflictCount)
                .newCheckpointChangeId(latestChangeId)
                .acks(acks)
                .build();
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
                .changes(changeDtos)
                .build();
    }

    private void updateCheckpoint(AppUser user, String deviceId, Long changeId, String appVersion) {
        SyncCheckpoint ckpt = checkpointRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseGet(() -> SyncCheckpoint.builder()
                        .user(user)
                        .deviceId(deviceId)
                        .build());

        ckpt.setLastSyncedChangeId(Math.max(ckpt.getLastSyncedChangeId(), changeId));
        ckpt.setLastSyncTimestamp(LocalDateTime.now());
        if (appVersion != null) {
            ckpt.setClientAppVersion(appVersion);
        }
        checkpointRepository.save(ckpt);
    }
}
