package com.manacommunity.api.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommuteBookingRequest {
    @Min(1)
    private int seatsBooked;
    private String pickupNote;
}
