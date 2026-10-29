package com.manacommunity.api.broadcast.dto;

import lombok.Data;

@Data
public class SendBroadcastRequest {
    private String title;
    private String message;
    private String category;
    private String priority;
    private String target;
    private String scheduledAt;
}
