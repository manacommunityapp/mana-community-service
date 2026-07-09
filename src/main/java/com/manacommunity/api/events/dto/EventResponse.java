package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String type;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String locationType;
    private String location;
    private String priceType;
    private Double price;
    private Integer capacity;
    private String imageUrl;
    private String organizerName;
    private String organizerContact;
    private Long createdById;
    private String createdByName;
    private Long communityId;
    private int attendees;
    private boolean isRegistered;
    private String createdAt;
}
