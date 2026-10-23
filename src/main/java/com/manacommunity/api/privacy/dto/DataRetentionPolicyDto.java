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
public class DataRetentionPolicyDto {
    private Long id;
    private Long communityId;
    private String dataCategory;
    private Integer retentionPeriodDays;
    private String actionOnExpiry;
    private Boolean isActive;
    private String description;
    private LocalDateTime updatedAt;
}
