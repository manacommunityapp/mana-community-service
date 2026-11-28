package com.manacommunity.api.social.events.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FestivalTicketDto {
    private String ticketId;
    private Long eventId;
    private Long userId;
    private String residentName;
    private String flatNumber;
    private int headcount;
    private int vegMeals;
    private int nonVegMeals;
    private Instant expiresAt;
    private String qrToken;
}
