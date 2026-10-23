package com.manacommunity.api.groupbuying.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDisputeResponse {
    private String id;
    private String orderId;
    private String dealId;
    private String dealTitle;
    private String residentName;
    private String flat;
    private String reason;
    private String requestedResolution;
    private BigDecimal claimAmount;
    private String description;
    private String status;
    private String createdAt;
    private String resolvedAt;
    private String vendorResponse;
}
