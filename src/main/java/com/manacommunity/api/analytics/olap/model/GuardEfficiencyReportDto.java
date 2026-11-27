package com.manacommunity.api.analytics.olap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardEfficiencyReportDto {

    private Long communityId;
    private LocalDate reportDate;
    private List<GateEfficiencyItem> gateBreakdowns;
    private double overallAverageApprovalSeconds;
    private double overallP95ApprovalSeconds;
    private List<String> flaggedSlowGates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateEfficiencyItem {
        private String gateId;
        private long totalEntries;
        private double avgApprovalSeconds;
        private double p95ApprovalSeconds;
        private double preAuthorizedRatio;
        private boolean slaViolated;
    }
}
