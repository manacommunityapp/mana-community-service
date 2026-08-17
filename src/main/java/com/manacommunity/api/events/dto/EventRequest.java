package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventRequest {

    @NotBlank
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
    private Long venueId;
    private String venue;
    private String city;
    private String category;
    private String status;
    private String paymentModes;
    private Integer maxAttendees;
}
