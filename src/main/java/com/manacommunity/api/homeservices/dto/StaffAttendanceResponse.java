package com.manacommunity.api.homeservices.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class StaffAttendanceResponse {
    private Long id;
    private Long staffId;
    private String staffName;
    private LocalDate date;
    private String status;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String checkInGate;
    private String checkOutGate;
    private String markedByName;
    private LocalDateTime createdAt;
}
