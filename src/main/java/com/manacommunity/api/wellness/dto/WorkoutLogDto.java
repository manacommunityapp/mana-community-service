package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutLogDto {
    private Long id;
    private Long userId;
    private Long exerciseCatalogId;
    private String exerciseName;
    private String exerciseCategory;
    private LocalDate workoutDate;
    private LocalTime startTime;
    private Integer durationMin;
    private Integer caloriesBurned;
    private Integer sets;
    private Integer repsPerSet;
    private BigDecimal weightKg;
    private BigDecimal distanceKm;
    private String source;
    private String notes;
    private BigDecimal metValue;
}
