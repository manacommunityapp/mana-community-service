package com.manacommunity.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommuteRideRequest {
    @NotBlank
    private String fromLocation;

    @NotBlank
    private String toLocation;

    private Double fromLat;
    private Double fromLng;
    private Double toLat;
    private Double toLng;

    @NotNull
    private LocalDateTime departureTime;

    @NotNull
    private String rideType;

    @Min(1)
    private int totalSeats;

    private Double pricePerSeat;
    private boolean free;
    private String vehicleType;
    private String vehicleNumber;
    private String notes;
    private boolean recurring;
    private String recurringDays;
    private LocalTime recurringTime;
    private boolean ladiesOnly;
}
