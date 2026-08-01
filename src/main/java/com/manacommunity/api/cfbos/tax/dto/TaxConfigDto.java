package com.manacommunity.api.cfbos.tax.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxConfigDto {
    private Long id;
    private String communityGstin;
    private String communityStateCode;
    private Boolean isGstRegistered;
    private BigDecimal defaultGstRate;
    private BigDecimal defaultCgstRate;
    private BigDecimal defaultSgstRate;
    private Integer financialYearStartMonth;
}
