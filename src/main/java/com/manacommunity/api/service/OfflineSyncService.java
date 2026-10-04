package com.manacommunity.api.service;

import com.manacommunity.api.dto.SyncPullResponse;
import com.manacommunity.api.dto.SyncPushRequest;
import com.manacommunity.api.dto.SyncPushResponse;
import com.manacommunity.api.model.SyncChangeLog;
import com.manacommunity.api.model.SyncMutation;
import com.manacommunity.api.repository.SyncChangeLogRepository;
import com.manacommunity.api.repository.SyncMutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfflineSyncService {

    @Autowired
    private SyncMutationRepository mutationRepository;

    @Autowired
    private SyncChangeLogRepository changeLogRepository;

    @Transactional
    public SyncPushResponse pushMutations(Long userId, SyncPushRequest request) {
        List<String> accepted = new ArrayList<>();
        List<String> conflicted = new ArrayList<>();

        for (SyncPushRequest.ClientMutation cm : request.getClientMutations()) {
            if (mutationRepository.existsByMutationId(cm.getClientMutationId())) {
                conflicted.add(cm.getClientMutationId());
                continue;
            }

            SyncMutation mutation = SyncMutation.builder()
                    .mutationId(cm.getClientMutationId())
                    .societyId(request.getSocietyId())
                    .userId(userId)
                    .entityType(cm.getEntityType())
                    .action(cm.getAction())
                    .payloadJson(cm.getPayloadJson())
                    .vectorTimestamp(cm.getClientVectorTimestamp())
                    .status("ACCEPTED")
                    .build();
            mutationRepository.save(mutation);

            Long nextTs = changeLogRepository.findMaxTimestamp(request.getSocietyId()) + 1;
            SyncChangeLog changeLog = SyncChangeLog.builder()
                    .societyId(request.getSocietyId())
                    .entityType(cm.getEntityType())
                    .action(cm.getAction())
                    .entityId(0L)
                    .payloadJson(cm.getPayloadJson())
                    .serverVectorTimestamp(nextTs)
                    .build();
            changeLogRepository.save(changeLog);

            accepted.add(cm.getClientMutationId());
        }

        Long checkpoint = changeLogRepository.findMaxTimestamp(request.getSocietyId());

        return SyncPushResponse.builder()
                .acceptedMutationIds(accepted)
                .conflictedMutationIds(conflicted)
                .serverCheckpoint(checkpoint)
                .status(conflicted.isEmpty() ? "OK" : "PARTIAL")
                .build();
    }

    public SyncPullResponse pullChanges(Long communityId, Long sinceCheckpoint, int limit) {
        List<SyncChangeLog> changes = changeLogRepository.findChangesSince(
                communityId, sinceCheckpoint, PageRequest.of(0, limit));

        List<SyncPullResponse.ChangeLogEntry> entries = changes.stream()
                .map(c -> SyncPullResponse.ChangeLogEntry.builder()
                        .changeLogId(c.getId())
                        .entityType(c.getEntityType())
                        .action(c.getAction())
                        .entityId(c.getEntityId())
                        .payloadJson(c.getPayloadJson())
                        .serverVectorTimestamp(c.getServerVectorTimestamp())
                        .build())
                .collect(Collectors.toList());

        Long newCheckpoint = changes.isEmpty() ? sinceCheckpoint :
                changes.get(changes.size() - 1).getServerVectorTimestamp();

        return SyncPullResponse.builder()
                .changes(entries)
                .newCheckpoint(newCheckpoint)
                .build();
    }
}
