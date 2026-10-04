package com.manacommunity.api.trip.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record TripResponse(
        Long id,
        String title,
        String description,
        String destination,
        String tripType,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxParticipants,
        int currentParticipants,
        BigDecimal estimatedCost,
        String meetingPoint,
        String status,
        String organizerName,
        Long organizerId,
        String notes,
        LocalDateTime createdAt
) {}
