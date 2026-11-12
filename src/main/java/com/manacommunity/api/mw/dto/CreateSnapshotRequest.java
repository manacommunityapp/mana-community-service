package com.manacommunity.api.mw.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class CreateSnapshotRequest {
    private LocalDate weekStartDate;
    private String sourceModule;
    private Integer demandUnits;
    private Integer fulfilledUnits;
    private BigDecimal avgPayRate;
    private Integer workerCount;
    private String notes;
}
