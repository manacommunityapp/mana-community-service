package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CposCrmActivityDto {
    private Long id;
    private String activityType;
    private String note;
    private String createdBy;
    private String callOutcome;     // CONNECTED | MISSED | BUSY (null for non-call activities)
    private Integer callDurationMin;
    private LocalDateTime createdAt;
}
