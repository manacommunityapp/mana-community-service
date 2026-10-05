package com.manacommunity.api.safety.dto;

import lombok.Data;

@Data
public class AnprSummaryResponse {
    private long totalEvents;
    private long totalEntries;
    private long totalExits;
    private long recognizedCount;
    private long unrecognizedCount;
    private long alertCount;
}
