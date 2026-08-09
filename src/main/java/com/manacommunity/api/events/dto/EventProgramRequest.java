package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventProgramRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String title;

    private String dayLabel;
    private String dayDate;
    private String programType;
    private String startTime;
    private String duration;
    private String venue;
    private String performer;
    private String judge;
    private Integer sortOrder;
    private Integer capacity;
    private Boolean requiresRegistration;
    private Boolean requiresApproval;
    private Boolean allowWaitlist;
    private Integer maxPerRegistration;
}
