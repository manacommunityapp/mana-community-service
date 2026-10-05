package com.manacommunity.api.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripRequest(
        @NotBlank @Size(max = 150) String title,
        String description,
        @NotBlank @Size(max = 200) String destination,
        String tripType,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        Integer maxParticipants,
        BigDecimal estimatedCost,
        @Size(max = 200) String meetingPoint,
        String notes
) {}
