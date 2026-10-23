package com.manacommunity.api.serviceplatform.material.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMaterialDto {
    private Long id;
    private Long providerId;
    private String itemName;
    private String itemCode;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private int stockQuantity;
    private boolean active;
}
