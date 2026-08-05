package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class EventVolunteerRequest {
    private Long eventId;
    private Long userId;
    private String userName;
    private String role;
    private String zone;
    private String shift;
}
