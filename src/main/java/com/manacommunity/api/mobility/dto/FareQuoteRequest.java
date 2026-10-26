package com.manacommunity.api.mobility.dto;

import com.manacommunity.api.mobility.model.MobilityServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FareQuoteRequest {
    @NotNull private MobilityServiceType serviceType;
    @NotNull private Double pickupLat;
    @NotNull private Double pickupLng;
    @NotNull private Double destLat;
    @NotNull private Double destLng;
}
