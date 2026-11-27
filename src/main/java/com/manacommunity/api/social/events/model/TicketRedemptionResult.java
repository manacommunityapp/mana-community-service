package com.manacommunity.api.social.events.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRedemptionResult {
    private boolean valid;
    private String ticketId;
    private Long eventId;
    private String flatNumber;
    private int headcount;
    private int vegMeals;
    private int nonVegMeals;
    private String rejectionReason;
}
