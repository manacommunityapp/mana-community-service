package com.manacommunity.api.sports.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TrainerSlotRequest {
    private Long trainerId;
    private String sport;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String slotType;        // INDIVIDUAL | GROUP
    private Integer maxCapacity;
    private Long venueId;
    private String venueName;
    private BigDecimal price;
    private String notes;
}
