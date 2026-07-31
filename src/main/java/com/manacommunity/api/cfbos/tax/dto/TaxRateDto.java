package com.manacommunity.api.cfbos.tax.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRateDto {
    private Long id;
    private String name;
    private String taxType;
    private BigDecimal rate;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
}
