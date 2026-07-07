package com.manacommunity.api.retail.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetailProductDto {

    private Long id;
    private String name;
    private String emoji;
    private String category;
    private BigDecimal unitPrice;
    private Integer reorderLevel;
    private Integer unitsOrdered;
    private Integer unitsSold;
}
