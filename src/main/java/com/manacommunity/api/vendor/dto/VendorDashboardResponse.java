package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VendorDashboardResponse {
    private long totalVendors;
    private long activeVendors;
    private long pendingRegistrations;
    private long totalBookings;
    private long activeBookings;
    private long completedBookings;
    private long openWorkOrders;
    private long activeContracts;
    private BigDecimal totalRevenue;
    private BigDecimal pendingPayments;
    private BigDecimal avgRating;
}
