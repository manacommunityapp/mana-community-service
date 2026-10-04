package com.manacommunity.api.homeservices.dto;

import com.manacommunity.api.homeservices.model.StaffAttendance;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class StaffAttendanceRequest {

    @NotNull
    private Long staffId;

    @NotNull
    private LocalDate date;

    @NotNull
    private StaffAttendance.AttendanceStatus status;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String checkInGate;
    private String checkOutGate;
}
