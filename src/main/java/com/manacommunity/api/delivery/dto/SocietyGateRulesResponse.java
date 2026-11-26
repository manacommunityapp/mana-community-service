package com.manacommunity.api.delivery.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyGateRulesResponse {
    private Long communityId;
    private String communityName;
    private Boolean vehiclesAllowedInside;
    private String twoWheelerParkingBay;
    private Integer speedLimitKmph;
    private String quietHours;
    private List<String> expressDeliveryGates;
    private String visitorHelpdeskPhone;
}
