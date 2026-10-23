package com.manacommunity.api.events.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledNotificationResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String type;
    private String typeLabel;
    private List<String> channels;
    private String scheduledAt;
    private String repeat;
    private String repeatLabel;
    private Integer customRepeatDays;
    private int recipients;
    private String status;
    private String message;
    private String sentAt;
    private String createdAt;
    private String createdBy;
}
