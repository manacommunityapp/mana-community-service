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
    private LocalDateTime createdAt;
}
