package com.manacommunity.api.homeservice.dto;

import com.manacommunity.api.homeservice.model.enums.HomeServiceBookingType;
import com.manacommunity.api.homeservice.model.enums.HomeServicePricingModel;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class HomeServiceBookingRequest {
    private String communityId;
    private String residentUserId;
    private String flatNumber;
    private String tower;
    private String workerId;
    private String categoryId;
    private HomeServiceBookingType bookingType;
    private HomeServicePricingModel pricingModel;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> recurringDays;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private String notes;
}
