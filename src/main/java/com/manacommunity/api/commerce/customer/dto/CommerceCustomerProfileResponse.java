package com.manacommunity.api.commerce.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommerceCustomerProfileResponse {
    private Long id;
    private Long userId;
    private String displayName;
    private String email;
    private String phoneNumber;
    private String customerStatus;
    private BigDecimal loyaltyPoints;
    private Integer totalOrders;
    private BigDecimal totalSpend;
    private Boolean isMember;
    private String preferredCategories;
    private Boolean kycVerified;
    private LocalDateTime kycVerifiedAt;
    private Long defaultAddressId;
    private List<CommerceCustomerAddressResponse> addresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
