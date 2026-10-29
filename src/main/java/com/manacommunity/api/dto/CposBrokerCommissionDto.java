package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CposBrokerCommissionDto {
    private Long id;
    private Long brokerId;
    private String brokerName;
    private String firmName;
    private Long leadId;
    private String leadName;
    private Integer budgetLakhs;
    private String leadStage;
    private BigDecimal commissionRate;
    private BigDecimal commissionLakhs;
    private String status;
    private LocalDateTime createdAt;
}
