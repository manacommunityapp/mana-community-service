package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteBookingResponse {
    private Long id;
    private Long passengerId;
    private String passengerName;
    private String passengerFlat;
    private String passengerPhoto;
    private int seatsBooked;
    private String pickupNote;
    private String status;
    private LocalDateTime createdAt;
}
