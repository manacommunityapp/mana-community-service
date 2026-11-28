package com.manacommunity.api.societyerp.model;

import lombok.*;

import java.math.BigDecimal;

/**
 * Society-level billing configuration and statutory tax rules.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceBillingConfig {

    private String societyId;
    private String societyName;
    private String societyGstin;
    private String vpaAddress;

    // Billing rates
    private BigDecimal baseRatePerSqFt; // e.g., 3.50
    private BigDecimal fixedSinkingFund; // e.g., 500.00
    private BigDecimal parkingSlotRate; // e.g., 300.00 per slot
    private BigDecimal waterRatePerUnit; // e.g., 45.00 per KL

    // Statutory Indian GST Rules (CBIC Circular No. 109/28/2019-GST)
    @Builder.Default
    private BigDecimal gstExemptionThreshold = new BigDecimal("7500.00"); // Exempt if <= 7,500/month
    @Builder.Default
    private BigDecimal gstRatePct = new BigDecimal("18.00"); // 18% (9% CGST + 9% SGST)
    @Builder.Default
    private boolean societyTurnoverAbove20Lakhs = true;

    // Late penalty & due dates
    @Builder.Default
    private int dueDayOffset = 10; // 10th of every month
    @Builder.Default
    private int graceDays = 5; // Up to 15th of month
    @Builder.Default
    private BigDecimal latePenaltyAnnualInterestPct = new BigDecimal("18.00"); // 18% p.a.
}
