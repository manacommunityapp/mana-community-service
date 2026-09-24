package com.manacommunity.api.dto;

import jakarta.validation.constraints.*;
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
    @Size(max = 255)
    private String fromLocation;

    @NotBlank
    @Size(max = 255)
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
    @Max(20)
    private int totalSeats;

    @Min(0)
    private Double pricePerSeat;
    private boolean free;

    @Size(max = 50)
    private String vehicleType;

    @Size(max = 20)
    private String vehicleNumber;

    @Size(max = 500)
    private String notes;
    private boolean recurring;
    private String recurringDays;
    private LocalTime recurringTime;
    private boolean ladiesOnly;
}
