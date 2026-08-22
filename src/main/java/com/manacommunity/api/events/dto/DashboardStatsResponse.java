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
    private double donationTotal;
    private double sponsorTotal;
    private long activeSponsorCount;
    private long pendingSponsorCount;
    private double foodPreparedPercentage;
    private long foodPlatesCount;
    private double auctionRevenue;
    private long auctionItemCount;
    // todaysScheduleCount = number of events happening today (startDate <= today <= endDate)
    private long todaysScheduleCount;
    // todaysDutyCount = number of volunteers assigned to events active today
    private long todaysDutyCount;
    private long pendingActionItemsCount;
}
