package com.manacommunity.api.cfbos.tax.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsnSacCodeDto {
    private Long id;
    private String code;
    private String description;
    private String codeType;
    private BigDecimal defaultGstRate;
    private Boolean isActive;
}
