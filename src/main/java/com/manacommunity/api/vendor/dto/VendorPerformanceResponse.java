package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VendorPerformanceResponse {
    private Long vendorId;
    private String businessName;
    private BigDecimal avgRating;
    private Integer totalRatings;
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private BigDecimal onTimeCompletionRate;
    private Integer responseTimeMinutes;
    private BigDecimal totalRevenue;
    private BigDecimal performanceScore;
    private String performanceTier;
}
