package com.manacommunity.api.wellnessbasket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessClusterDto {
    private Long id;
    private String societyName;
    private String block;
    private String city;
    private BigDecimal lat;
    private BigDecimal lng;
    private String status; // PENDING, ACTIVE, SUSPENDED
    private int currentSubscriberCount;
    private int activationThreshold;
    private int maxCapacity;
    private LocalTime deliveryStartTime;
    private LocalTime deliveryEndTime;
    private boolean isActivated;
    private int subscribersNeededForActivation;
}
