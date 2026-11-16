package com.manacommunity.api.wellness.dto;

import lombok.Data;

import java.util.List;

@Data
public class TrainerProgramDto {
    private Long id;
    private String title;
    private String description;
    private String difficulty;
    private String targetGoal;
    private Integer durationWeeks;
    private Boolean isPublic;
    private Long createdByUserId;
    private String thumbnailUrl;
    private List<TrainerProgramDayDto> days;
    private Boolean enrolled;
    private String enrollmentStatus;
    private Integer currentWeek;
    private Integer currentDay;
}
