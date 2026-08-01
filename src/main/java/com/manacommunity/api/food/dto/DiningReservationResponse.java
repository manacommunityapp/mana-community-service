package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class DiningReservationResponse {
    private Long id;
    private Long userId;
    private Long restaurantId;
    private String restaurantName;
    private String reservationType;
    private LocalDate date;
    private LocalTime time;
    private Integer partySize;
    private Long tableId;
    private String status;
    private String specialRequests;
    private String occasion;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
