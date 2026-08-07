package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class ActivityRegistrationResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private Long userId;
    private String userName;
    private int headCount;
    private String status;
    private int spotsLeft;
    private String registeredAt;
}
