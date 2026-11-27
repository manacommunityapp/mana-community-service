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
public class PeakDeliveryHourAnalysisDto {

    private Long communityId;
    private LocalDate reportDate;
    private int primaryPeakHour;      // e.g. 19 for 7:00 PM - 8:00 PM
    private long primaryPeakVolume;
    private int secondaryPeakHour;    // e.g. 13 for 1:00 PM - 2:00 PM lunch spike
    private long secondaryPeakVolume;
    private double peakToAverageRatio;
    private List<HourlyBucket> hourlyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyBucket {
        private int hour; // 0..23
        private long totalDeliveries;
        private double percentageOfDailyDeliveries;
    }
}
