package com.manacommunity.api.telephony.cascade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallHealthMetricsDto {

    private long iceConnectionDurationMs;
    private boolean iceConnected;
    private double packetLossPercentage;
    private long roundTripTimeMs;
    private long jitterMs;
}
