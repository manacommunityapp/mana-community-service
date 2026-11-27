package com.manacommunity.api.water.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NightFlowLeakAnalysisResult {

    public enum Severity {
        NORMAL,
        POSSIBLE_MINOR_LEAK,
        CRITICAL_UNDERGROUND_LEAK
    }

    private Long communityId;
    private LocalDate analysisDate;
    private boolean leakDetected;
    private Severity severity;
    private double minimumNightFlowLpm;       // Liters per minute
    private double expectedBaselineLpm;       // Based on flats
    private double estimatedLeakRateLpm;      // Excess LPM
    private double estimatedLeakRateLph;      // Excess Liters Per Hour
    private double dailyProjectedLossLiters;  // Loss if unaddressed for 24h
    private String alertMessage;
}
