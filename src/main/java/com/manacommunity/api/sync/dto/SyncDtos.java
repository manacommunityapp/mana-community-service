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

        /** Monotonic per-tablet counter to detect out-of-window batch replays. */
        private Long localSeq;

        /** Per-pass rotating TOTP step or single-use nonce to block offline screenshot replay. */
        private String passNonce;

        /** Pass code / school-bus badge token when entityId is omitted offline. */
        private String passCode;

        /** Gate identifier where offline clearance happened. */
        private String gateName;

        /** Optional HMAC-SHA256 / keystore signature produced by the offline gate tablet. */
        private String deviceSignature;
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
        private String conflictReason;
        private boolean offlineOverride;
        private boolean anomalyLogged;
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
        private int replayRejectedCount;
        private int anomalyCount;
        private Long newCheckpointChangeId;
        private Long newCheckpointSeq;
        private Long serverEpochMs;
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
        private Long serverEpochMs;
        private List<SyncChangeItemDto> changes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateDeviceRegisterRequest {
        @NotBlank(message = "Device ID is required")
        private String deviceId;
        @NotBlank(message = "Gate name is required")
        private String gateName;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignedPreAuthEntryDto {
        private SyncEntityType entityType;
        private Long entityId;
        private String passCode;
        private String subjectName;
        private String flatOrStop;
        private String vehicleOrBusRef;
        private String status;
        private String validFrom;
        private String validUntil;
        private boolean singleUse;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignedGateRosterBundle {
        private Long communityId;
        private String deviceId;
        private String gateName;
        private String signingSecret;
        private Long rosterVersion;
        private Long serverEpochMs;
        private Long expiresAtEpochMs;
        private String bundleSignature;
        private List<SignedPreAuthEntryDto> entries;
        private List<String> revokedPassCodes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateAnomalyDto {
        private Long id;
        private String deviceId;
        private Long localSeq;
        private String clientMutationId;
        private SyncEntityType entityType;
        private Long entityId;
        private String passCode;
        private String passNonce;
        private String gateName;
        private LocalDateTime occurredAt;
        private LocalDateTime reconciledAt;
        private SyncStatus syncStatus;
        private boolean offlineOverride;
        private String conflictReason;
        private boolean resolved;
        private String resolutionNote;
    }
}
