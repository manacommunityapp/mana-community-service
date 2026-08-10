package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {

    private long totalEvents;
    private long upcomingEvents;
    private long totalRegistrations;
    private long totalVolunteers;
    private double totalRevenue;
    private double totalExpenses;
    private double foodPreparedPercentage;
    private long foodPlatesCount;
    private double auctionRevenue;
    private long auctionItemCount;
    private long todaysScheduleCount;
    private long todaysDutyCount;
    private long pendingActionItemsCount;
}
