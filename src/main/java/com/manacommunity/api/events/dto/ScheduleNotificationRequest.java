package com.manacommunity.api.events.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleNotificationRequest {
    private Long eventId;
    private String type;
    private List<String> channels;
    private boolean sendNow;
    private String scheduledAt;
    private String repeat;
    private Integer customRepeatDays;
    private boolean sendToAll;
    private List<Long> recipientIds;
    private String customMessage;
}
