package com.manacommunity.api.events.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EventProgramRequest {
    private Long eventId;
    private String title;
    private String dayLabel;
    private LocalDate dayDate;
    private String programType;
    private String activityType;
    private String startTime;
    private String duration;
    private String venue;
    private String performer;
    private String judge;
    private Integer sortOrder;
    private Integer capacity;
    private Boolean requiresRegistration;
    private Boolean waitlistEnabled;
}
