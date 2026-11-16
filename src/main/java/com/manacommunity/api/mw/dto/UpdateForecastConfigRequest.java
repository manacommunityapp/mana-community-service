package com.manacommunity.api.mw.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class UpdateForecastConfigRequest {
    private Integer horizonWeeks;
    private Integer minWorkersPerShift;
    private BigDecimal peakDayMultiplier;
    private String sourceModules;
    private Boolean autoSnapshotEnabled;
}
