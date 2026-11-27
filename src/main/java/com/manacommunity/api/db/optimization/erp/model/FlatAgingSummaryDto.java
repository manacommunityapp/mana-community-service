package com.manacommunity.api.db.optimization.erp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatAgingSummaryDto {

    private Long flatId;
    private String unitNumber;
    private String residentName;
    private BigDecimal currentDue;      // 0 - 30 days
    private BigDecimal due30Days;       // 31 - 60 days
    private BigDecimal due60Days;       // 61 - 90 days
    private BigDecimal due90PlusDays;   // > 90 days
    private BigDecimal totalOutstanding;
    private LocalDateTime lastPaymentDate;
}
