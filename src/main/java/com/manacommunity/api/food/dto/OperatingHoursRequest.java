package com.manacommunity.api.food.dto;

import lombok.Data;

@Data
public class OperatingHoursRequest {
    private String dayOfWeek;
    private String openTime;
    private String closeTime;
    private Boolean isClosed;
}
