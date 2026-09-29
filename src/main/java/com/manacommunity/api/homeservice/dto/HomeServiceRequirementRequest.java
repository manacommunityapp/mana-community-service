package com.manacommunity.api.homeservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class HomeServiceRequirementRequest {
    private String communityId;
    private String residentUserId;
    private String residentName;
    private String flatNumber;
    private String tower;
    private String categoryId;
    private String frequency;
    private List<String> preferredDays;
    private LocalDate startDate;
    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String description;
}
