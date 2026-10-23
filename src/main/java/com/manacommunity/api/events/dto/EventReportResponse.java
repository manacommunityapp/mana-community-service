package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReportResponse {

    private Long eventId;
    private String eventTitle;
    private long totalRegistrations;
    private long generalRegistrationsCount;
    private long poojaRegistrationsCount;
    private long activityRegistrationsCount;
    private long bookingRegistrationsCount;
    private long mealRegistrationsCount;
    private long totalVolunteers;
    private double totalDonations;
    private double totalSponsorships;
    private double totalExpenses;
    private double poojaRevenue;
    private double totalRevenue;
    private double netRevenue;
    private long totalGalleryItems;
    private long totalTasks;
    private long completedTasks;
    private long totalPrograms;
    private long totalDevoteesHeadcount;
    private long totalCheckedIn;
}

