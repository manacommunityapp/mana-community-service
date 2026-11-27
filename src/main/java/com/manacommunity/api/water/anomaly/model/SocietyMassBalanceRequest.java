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
public class SocietyMassBalanceRequest {

    private Long communityId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private double initialStorageLiters;
    private double finalStorageLiters;
    private double verifiedTankerInflowLiters;
    private double borewellInflowLiters;
    private double municipalInflowLiters;
    private double towerDistributionOutflowLiters;
    private double treatmentLossLiters; // STP/WTP backwash, reject, etc.
}
