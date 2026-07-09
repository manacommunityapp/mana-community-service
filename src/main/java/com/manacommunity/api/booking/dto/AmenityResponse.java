package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AmenityResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private Integer maxCapacity;
    private String openTime;
    private String closeTime;
    private Integer slotDurationMinutes;
    private boolean active;
    private Long communityId;
}
