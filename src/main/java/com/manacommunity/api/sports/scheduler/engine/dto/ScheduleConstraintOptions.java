package com.manacommunity.api.sports.scheduler.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConstraintOptions {

    @Builder.Default
    private boolean enforceFlatSeparation = true;

    @Builder.Default
    private boolean enforceTowerDispersion = true;

    @Builder.Default
    private boolean enforceRatingBalancing = true;

    @Builder.Default
    private boolean enforceFairByes = true;

    @Builder.Default
    private int minRestMinutes = 60;

    @Builder.Default
    private int matchDurationMinutes = 45;

    @Builder.Default
    private int changeoverBufferMinutes = 15;

    @Builder.Default
    private LocalTime dayStartTime = LocalTime.of(8, 0);

    @Builder.Default
    private LocalTime dayEndTime = LocalTime.of(21, 0);

    @Builder.Default
    private LocalDate tournamentStartDate = LocalDate.now().plusDays(1);
}
