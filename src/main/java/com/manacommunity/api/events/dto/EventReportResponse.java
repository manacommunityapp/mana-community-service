package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventReportResponse {

    private Long eventId;
    private String eventTitle;
    private long totalRegistrations;
    private long totalVolunteers;
    private double totalDonations;
    private double totalSponsorships;
    private double totalExpenses;
    private double netRevenue;
    private long totalGalleryItems;
    private long totalTasks;
    private long completedTasks;
    private long totalPrograms;
}
