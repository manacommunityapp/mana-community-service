package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class FoodEventRequest {
    @NotBlank
    private String name;
    private String description;
    private String eventType;
    private String venue;
    @NotNull
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime registrationDeadline;
}
