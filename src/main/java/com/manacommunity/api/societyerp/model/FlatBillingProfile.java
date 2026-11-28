package com.manacommunity.api.societyerp.model;

import lombok.*;

import java.math.BigDecimal;

/**
 * Property profile containing area, resident details, utility consumption,
 * and current Advance Maintenance Wallet balance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatBillingProfile {

    private String flatNumber;
    private String tower;
    private double carpetAreaSqFt;
    private String residentId;
    private String residentName;
    private String residentPhone;

    @Builder.Default
    private int parkingSlotCount = 0;

    @Builder.Default
    private double waterUnitsConsumed = 0.0; // in kilolitres

    @Builder.Default
    private BigDecimal arrearsFromPreviousPeriod = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal advanceWalletBalance = BigDecimal.ZERO; // Prepaid balance
}
