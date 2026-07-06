package com.manacommunity.api.retail.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetailOrderDto {

    private Long id;
    private String code;
    private String type;
    private Long partyId;
    private String partyName;
    private String orderDate;
    private String status;
    private List<LineDto> items;
    private BigDecimal total;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineDto {
        private Long productId;
        private String productName;
        private Integer qty;
        private BigDecimal unitPrice;
    }
}
