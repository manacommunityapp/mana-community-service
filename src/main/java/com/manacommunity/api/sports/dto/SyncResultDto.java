package com.manacommunity.api.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResultDto {
    private Long formId;
    private int polledCount;
    private int importedCount;
    private int quarantinedCount;
    private int rejectedCount;
    private String message;
}
