package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DiningReservationRequest {
    @NotNull
    private Long restaurantId;
    private String reservationType;
    @NotNull
    private LocalDate date;
    @NotNull
    private LocalTime time;
    @NotNull
    private Integer partySize;
    private Long tableId;
    private String specialRequests;
    private String occasion;
}
