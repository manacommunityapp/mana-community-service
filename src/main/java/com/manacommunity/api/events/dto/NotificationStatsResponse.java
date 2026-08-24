package com.manacommunity.api.events.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {
    private long total;
    private long scheduled;
    private long sent;
    private long paused;
    private long failed;
    private long totalRecipients;
    private Map<String, Long> channelBreakdown;
}
