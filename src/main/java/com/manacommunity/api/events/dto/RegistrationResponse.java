package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private String status;
    private String registeredAt;
    private Boolean checkedIn;
    private String checkedInAt;
}
