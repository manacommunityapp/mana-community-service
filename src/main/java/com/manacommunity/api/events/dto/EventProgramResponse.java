package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class EventProgramResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String dayLabel;
    private String dayDate;
    private String title;
    private String programType;
    private String activityType;
    private String startTime;
    private String duration;
    private String venue;
    private String performer;
    private String judge;
    private int sortOrder;
    private Integer capacity;
    private boolean requiresRegistration;
    private int registeredCount;
    private int spotsLeft;
    private String slotStatus;
    private boolean waitlistEnabled;
    private String createdAt;
}
