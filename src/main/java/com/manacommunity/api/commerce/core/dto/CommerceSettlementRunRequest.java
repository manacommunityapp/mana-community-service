package com.manacommunity.api.commerce.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommerceSettlementRunRequest {
    /** ISO-8601 date e.g. 2026-10-01 — inclusive start of cycle. */
    @NotBlank
    private String cycleStart;

    /** ISO-8601 date e.g. 2026-10-31 — inclusive end of cycle. */
    @NotBlank
    private String cycleEnd;

    /** Either sellerId OR vendorId must be provided. */
    private Long sellerId;
    private String vendorId;

    /** Platform fee percentage (0–100), default 2.5. */
    private Double platformFeePercent = 2.5;

    /** Optional note for this settlement run. */
    private String adminNote;
}
