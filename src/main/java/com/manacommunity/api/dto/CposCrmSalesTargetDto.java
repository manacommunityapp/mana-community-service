package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CposCrmSalesTargetDto {
    private Long id;
    private String agentName;
    private int periodMonth;
    private int periodYear;
    private int targetLeadsClosed;
    private int targetRevenueLakhs;
    // actuals filled at query time
    private long actualLeadsClosed;
    private long actualRevenueLakhs;
    private int closedPct;
    private int revenuePct;
}
