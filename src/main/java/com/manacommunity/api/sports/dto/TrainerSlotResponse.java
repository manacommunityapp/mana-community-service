package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class TrainerSlotResponse {
    private Long id;
    private Long trainerId;
    private String trainerName;
    private String sport;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String slotType;
    private Integer maxCapacity;
    private Integer bookedCount;
    private Long venueId;
    private String venueName;
    private BigDecimal price;
    private String status;
    private String notes;
}
