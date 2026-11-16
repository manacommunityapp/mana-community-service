package com.manacommunity.api.freshkitchen.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizedComboResponseDto {

    private String comboCode;
    private String comboName;
    private BigDecimal basePrice;
    private BigDecimal priceAdjustment;
    private BigDecimal finalPrice;

    @Builder.Default
    private List<CustomizedItemDetail> itemBreakdown = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomizedItemDetail {
        private String originalProductId;
        private String selectedProductId;
        private String selectedProductName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private String itemRole;
    }
}
