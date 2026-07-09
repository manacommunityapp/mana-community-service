package com.manacommunity.api.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketRequest {

    @NotBlank
    private String subject;

    @NotBlank
    private String description;

    private String category;
    private String priority;
}
