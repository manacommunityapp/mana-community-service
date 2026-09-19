package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteRideResponse {
    private Long id;
    private Long driverId;
    private String driverName;
    private String driverFlat;
    private String driverPhoto;
    private String fromLocation;
    private String toLocation;
    private Double fromLat;
    private Double fromLng;
    private Double toLat;
    private Double toLng;
    private LocalDateTime departureTime;
    private String rideType;
    private int totalSeats;
    private int availableSeats;
    private Double pricePerSeat;
    private boolean free;
    private String vehicleType;
    private String vehicleNumber;
    private String notes;
    private String status;
    private boolean recurring;
    private String recurringDays;
    private LocalTime recurringTime;
    private boolean ladiesOnly;
    private int bookingCount;
    private boolean isMyRide;
    private boolean hasBooked;
    private String myBookingStatus;
    private List<CommuteBookingResponse> bookings;
    private LocalDateTime createdAt;
}
