package com.manacommunity.api.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSalesOrderDto {

    private Long id;
    private String code;
    private String status;
    private Long customerId;
    private String customerName;
    private LocalDate docDate;
    private LocalDate dueDate;
    private String notes;
    private String terms;
    private boolean taxInclusive;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal otherCharges;
    private BigDecimal grandTotal;
    private List<Line> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String item;
        private String description;
        private Integer qty;
        private BigDecimal cost;
        private BigDecimal disc;
        private BigDecimal tax;
        private BigDecimal lineTotal;
    }
}
