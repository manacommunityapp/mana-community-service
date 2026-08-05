package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventSponsorRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String name;

    private String tier;
    private Double amountPledged;
    private Double amountReceived;
    private String logoUrl;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String status;
}
