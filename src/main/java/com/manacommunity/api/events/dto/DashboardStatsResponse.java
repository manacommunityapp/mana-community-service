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
}
