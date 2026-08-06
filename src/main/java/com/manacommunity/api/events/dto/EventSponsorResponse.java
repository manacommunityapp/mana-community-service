package com.manacommunity.api.events.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventSponsorResponse {


    private Long id;
    private Long eventId;
    private String eventTitle;
    private String name;
    private String tier;
    private Double amountPledged;
    private Double amountReceived;
    private String logoUrl;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String status;
    private String createdAt;
}
