package com.manacommunity.api.wellness.dto;

import lombok.Data;

@Data
public class TrainerProgramDayDto {
    private Long id;
    private Long programId;
    private Integer weekNumber;
    private Integer dayNumber;
    private String dayName;
    private Boolean isRestDay;
    private String focus;
    private String exercises;
    private Integer totalDurationMin;
    private String notes;
}
