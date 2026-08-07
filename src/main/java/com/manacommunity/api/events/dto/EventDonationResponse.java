package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventDonationResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private Double amount;
    private String paymentMethod;
    private String transactionRef;
    private String note;
    private boolean anonymous;
    private String recordedByName;
    private String createdAt;
}
