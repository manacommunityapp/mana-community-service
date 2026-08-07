package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventDonationRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String donorName;

    private String donorEmail;
    private String donorPhone;

    @NotNull
    private Double amount;

    private String paymentMethod;
    private String transactionRef;
    private String note;
    private boolean anonymous;
}
