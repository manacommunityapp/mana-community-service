package com.manacommunity.api.cfbos.tax.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsSectionDto {
    private Long id;
    private String sectionCode;
    private String description;
    private BigDecimal individualRate;
    private BigDecimal companyRate;
    private BigDecimal thresholdAmount;
    private Boolean isActive;
}
