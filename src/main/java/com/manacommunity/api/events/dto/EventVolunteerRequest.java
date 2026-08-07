package com.manacommunity.api.events.dto;


import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class EventVolunteerRequest {

    @NotNull
    private Long eventId;

    @NotNull
    private Long userId;


    private String userName;

    private String role;
    private String zone;
    private String shift;
}
