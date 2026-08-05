package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventProgramResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String dayLabel;
    private String dayDate;
    private String title;
    private String programType;
    private String startTime;
    private String duration;
    private String venue;
    private String performer;
    private String judge;
    private int sortOrder;
    private String createdAt;
}
