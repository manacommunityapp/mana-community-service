package com.manacommunity.api.homeservices.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ServicePackageRequest {

    @NotNull
    private Long staffId;

    private String flatNumber;
    private List<String> services;
    private BigDecimal monthlySalary;
    private LocalDate nextDueDate;
}
