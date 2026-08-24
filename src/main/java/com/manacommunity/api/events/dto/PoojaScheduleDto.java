package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Read-only view of a PoojaSchedule including live availability counts. */
@Data
@Builder
public class PoojaScheduleDto {
    private Long id;
    private Long poojaId;
    private String poojaName;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int familyCapacity;
    private int devoteeCapacity;
    private PoojaScheduleStatus status;

    /** Available family slots right now (capacity - confirmed - active_reserved). */
    private int availableFamilies;

    /** Available devotee slots right now. */
    private int availableDevotees;

    private int nextTokenSeq;
}
