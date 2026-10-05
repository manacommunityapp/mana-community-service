package com.manacommunity.api.commerce.core.dto;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceDisputeDto {
    private Long id;
    private String disputeCode;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userName;
    private CommerceChannel channel;
    private String reason;
    private String description;
    private String requestedResolution;
    private BigDecimal claimAmount;
    private String status;
    private String vendorResponse;
    private String resolvedAt;
    private String createdAt;
}