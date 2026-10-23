package com.manacommunity.api.homeservices.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServicePackageResponse {
    private Long id;
    private Long staffId;
    private String staffName;
    private Long residentId;
    private String residentName;
    private String flatNumber;
    private List<String> services;
    private BigDecimal monthlySalary;
    private String paymentStatus;
    private LocalDate lastPaidDate;
    private LocalDate nextDueDate;
    private LocalDateTime createdAt;
}
