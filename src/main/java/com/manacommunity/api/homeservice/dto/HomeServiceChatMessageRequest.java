package com.manacommunity.api.homeservice.dto;

import com.manacommunity.api.homeservice.model.enums.HomeServiceSenderType;
import lombok.Data;

@Data
public class HomeServiceChatMessageRequest {
    private String bookingId;
    private String workerId;
    private String residentUserId;
    private HomeServiceSenderType senderType;
    private String senderName;
    private String messageText;
}
