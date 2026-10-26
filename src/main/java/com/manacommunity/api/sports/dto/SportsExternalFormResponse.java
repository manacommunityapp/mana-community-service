package com.manacommunity.api.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsExternalFormResponse {
    private Long id;
    private Long eventId;
    private String eventName;
    private Long communityId;
    private String provider;
    private String externalFormId;
    private String responderUrl;
    private String editUrl;
    private String title;
    private String description;
    private String webhookToken;
    private String webhookUrl;
    private String status;
    private String approvalMode;
    private List<FieldMappingDto> fieldMappings;
    private SyncStateDto syncState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncStateDto {
        private String syncMode;
        private LocalDateTime lastSyncTimestamp;
        private Integer totalResponsesPolled;
        private Integer totalImported;
        private Integer totalQuarantined;
        private Integer totalRejected;
        private String syncStatus;
        private String syncError;
    }
}
