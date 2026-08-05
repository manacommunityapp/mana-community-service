package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class EventVolunteerResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private String role;
    private String zone;
    private String shift;
    private String status;
    private String checkInTime;
    private String checkOutTime;
    private String createdAt;
}
