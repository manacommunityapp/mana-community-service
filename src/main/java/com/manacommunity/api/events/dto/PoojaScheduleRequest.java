package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Admin request body for creating/updating a EventPoojaSchedule. */
@Data
public class PoojaScheduleRequest {

    @NotNull(message = "poojaId is required")
    private Long poojaId;

    @NotNull(message = "scheduleDate is required")
    private LocalDate scheduleDate;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    private LocalTime endTime;

    @Min(value = 1, message = "familyCapacity must be at least 1")
    private Integer familyCapacity;

    @Min(value = 1, message = "devoteeCapacity must be at least 1")
    private Integer devoteeCapacity;

    private PoojaScheduleStatus status;
}
