package com.manacommunity.api.sync.dto;

import com.manacommunity.api.sync.SyncEnums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class SyncDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientMutationItem {
        @NotBlank(message = "Mutation ID is required")
        private String clientMutationId;

        @NotNull(message = "Entity type is required")
        private SyncEntityType entityType;

        private Long entityId;

        @NotNull(message = "Operation is required")
        private SyncOperation operation;

        @NotBlank(message = "Payload is required")
        private String payloadJson;

        @NotNull(message = "Client timestamp is required")
        private LocalDateTime clientTimestamp;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushSyncBatchRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Device ID is required")
        private String deviceId;

        @NotNull(message = "Community ID is required")
        private Long communityId;

        private String clientAppVersion;
        private Long lastKnownChangeId;
        private List<ClientMutationItem> mutations;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MutationSyncAck {
        private String clientMutationId;
        private Long serverChangeId;
        private Long entityId;
        private SyncStatus status;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushSyncBatchResult {
        private String deviceId;
        private int totalProcessed;
        private int appliedCount;
        private int conflictCount;
        private Long newCheckpointChangeId;
        private List<MutationSyncAck> acks;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PullSyncRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Device ID is required")
        private String deviceId;

        @NotNull(message = "Community ID is required")
        private Long communityId;

        @Builder.Default
        private Long sinceChangeId = 0L;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncChangeItemDto {
        private Long changeId;
        private SyncEntityType entityType;
        private Long entityId;
        private SyncOperation operation;
        private String payloadJson;
        private LocalDateTime serverTimestamp;
        private Long version;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PullSyncResponse {
        private Long latestChangeId;
        private boolean hasMore;
        private List<SyncChangeItemDto> changes;
    }
}
