package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepLogDto {
    private Long id;
    private Long userId;
    private LocalDate logDate;
    private Integer stepCount;
    private BigDecimal distanceKm;
    private Integer activeMinutes;
    private String source;
    private Long deviceId;
}
