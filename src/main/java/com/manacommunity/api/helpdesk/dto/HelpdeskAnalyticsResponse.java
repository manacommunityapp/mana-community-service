package com.manacommunity.api.helpdesk.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpdeskAnalyticsResponse {
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long escalatedTickets;
    private double slaCompliancePercentage;
    private double averageResolutionTimeHours;
    private double averageCsatRating;
    private Map<String, Long> ticketsByCategory;
    private Map<String, Long> ticketsByPriority;
}
