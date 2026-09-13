package com.manacommunity.api.dto.karate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalTime;

@Data
public class KarateBatchRequest {

    @NotBlank
    private String batchName;

    /** Comma-separated day abbreviations: MON,TUE,WED,THU,FRI,SAT,SUN */
    private String daysOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;
    private Long venueId;
    private Long courtId;
    private Integer maxStudents;
    private Integer attendanceThreshold;
    private Boolean autoGenerateClasses;
}
