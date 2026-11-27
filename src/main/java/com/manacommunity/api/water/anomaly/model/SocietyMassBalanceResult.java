package com.manacommunity.api.water.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyMassBalanceResult {

    public enum AuditStatus {
        BALANCED,
        ACCEPTABLE_LOSS,
        HIGH_UNACCOUNTED_WATER_LOSS
    }

    private Long communityId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private double totalInflowLiters;
    private double totalOutflowLiters;
    private double actualStorageDeltaLiters;
    private double theoreticalStorageDeltaLiters;
    private double unaccountedWaterLiters;
    private double unaccountedLossPercentage;
    private AuditStatus auditStatus;
    private String auditSummary;
}
