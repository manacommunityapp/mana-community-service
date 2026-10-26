package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CposBrokerDto {
    private Long id;
    private String brokerName;
    private String firmName;
    private String phone;
    private String email;
    private BigDecimal commissionRate;
    private int totalLeads;
    private int closedLeads;
    private BigDecimal totalCommissionLakhs;
    private List<CposBrokerCommissionDto> commissions;
}
