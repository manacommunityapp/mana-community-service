package com.manacommunity.api.privacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDeletionRequestDto {
    private Long id;
    private Long userId;
    private Long communityId;
    private String status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private Long processedBy;
    private String notes;
}
