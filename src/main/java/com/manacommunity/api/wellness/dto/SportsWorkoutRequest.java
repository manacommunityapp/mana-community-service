package com.manacommunity.api.wellness.dto;

import lombok.Data;

@Data
public class SportsWorkoutRequest {
    private Long bookingId;
    private String sportType;
    private Integer durationMin;
    private String workoutDate;
    private String notes;
}
