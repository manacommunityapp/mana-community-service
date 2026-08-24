package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Admin request body for creating/updating a PoojaSchedule. */
@Data
public class PoojaScheduleRequest {
    private Long poojaId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer familyCapacity;
    private Integer devoteeCapacity;
    private PoojaScheduleStatus status;
}
