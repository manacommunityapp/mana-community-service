package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {

    private long totalResources;
    private long totalBookings;
    private long todayBookings;
    private long activeBookings;
    private double occupancyRate;
    private BigDecimal revenue;
    private double cancellationRate;
    private List<ResourceBookingResponse> recentBookings;
}
