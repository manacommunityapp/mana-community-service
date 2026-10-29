package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterLogDto {
    private Long id;
    private Long userId;
    private LocalDate logDate;
    private LocalTime logTime;
    private Integer amountMl;
    private String notes;
}
