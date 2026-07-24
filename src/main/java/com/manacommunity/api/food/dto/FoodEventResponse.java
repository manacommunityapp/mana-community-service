package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class FoodEventResponse {
    private Long id;
    private String name;
    private String description;
    private String eventType;
    private String venue;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime registrationDeadline;
    private String status;
    private Integer registrationCount;
    private Integer spotsLeft;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
