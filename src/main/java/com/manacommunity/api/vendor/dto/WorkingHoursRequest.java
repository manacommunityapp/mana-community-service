package com.manacommunity.api.vendor.dto;

import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class WorkingHoursRequest {
    private List<DaySchedule> schedule;

    @Data
    public static class DaySchedule {
        private Integer dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean isWorkingDay;
    }
}
