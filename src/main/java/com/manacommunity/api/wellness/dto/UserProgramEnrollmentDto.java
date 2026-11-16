package com.manacommunity.api.wellness.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProgramEnrollmentDto {
    private Long id;
    private Long programId;
    private String programTitle;
    private String programDifficulty;
    private Integer programDurationWeeks;
    private LocalDate startDate;
    private Integer currentWeek;
    private Integer currentDay;
    private String status;
    private TrainerProgramDayDto todaysDay;
}
