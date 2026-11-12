package com.manacommunity.api.mw.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkHistoryRequest {
    private String employerName;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}
