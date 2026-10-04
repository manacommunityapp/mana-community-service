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
public class WorkOrderMaterialDto {
    private Long id;
    private Long workOrderId;
    private Long materialId;
    private String materialName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
