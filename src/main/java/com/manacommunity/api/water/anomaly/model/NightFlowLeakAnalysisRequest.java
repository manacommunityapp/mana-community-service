package com.manacommunity.api.water.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NightFlowLeakAnalysisRequest {

    private Long communityId;
    private LocalDate analysisDate;
    private LocalDateTime windowStartTime; // e.g., 02:00 AM
    private LocalDateTime windowEndTime;   // e.g., 04:30 AM
    private int numberOfFlats;
    private List<Double> flowMeterReadingsLpm; // Liters per minute recorded across 15-min intervals
    private boolean pumpTransferActive;
    private boolean fireLinePressureDropDetected;
}
