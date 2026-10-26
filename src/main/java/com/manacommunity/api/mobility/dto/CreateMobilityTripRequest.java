package com.manacommunity.api.mobility.dto;

import com.manacommunity.api.mobility.model.MobilityServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMobilityTripRequest {
    @NotNull  private MobilityServiceType serviceType;
    @NotNull  private Double pickupLat;
    @NotNull  private Double pickupLng;
    @NotBlank private String pickupAddress;
    @NotNull  private Double destLat;
    @NotNull  private Double destLng;
    @NotBlank private String destAddress;
              private String quoteId;
    @NotBlank private String idempotencyKey;
              private String paymentMethod = "CASH";
              private String promoCode;
}
