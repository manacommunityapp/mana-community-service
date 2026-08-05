package com.manacommunity.api.events.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EventTaskRequest {
    private Long eventId;
    private String title;
    private String description;
    private String phase;
    private String priority;
    private String assigneeName;
    private Long assigneeId;
    private LocalDate dueDate;
}
