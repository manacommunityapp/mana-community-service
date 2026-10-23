package com.manacommunity.api.serviceplatform.scheduling.dto;

import lombok.*;
import java.time.LocalTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderScheduleDto {
    private Long id;
    private Long providerId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private Integer bufferTimeMinutes;
    private Integer maxParallelJobs;
    private Boolean isActive;
}
