package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class EventRegistrationRequest {
    private String ticketType;
    private Integer quantity;
    private String name;
    private String email;
    private String phone;
    private String notes;
    private Boolean agreed;
}
