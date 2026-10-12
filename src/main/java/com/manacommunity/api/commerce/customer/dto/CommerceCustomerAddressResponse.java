package com.manacommunity.api.commerce.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CommerceCustomerAddressResponse {
    private Long id;
    private String label;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
