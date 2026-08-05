package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class EventTaskResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String title;
    private String description;
    private String phase;
    private String priority;
    private String assigneeName;
    private Long assigneeId;
    private String dueDate;
    private boolean done;
    private String createdAt;
}
