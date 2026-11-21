package com.manacommunity.api.finance.compliance.dto;

import lombok.Data;

@Data
public class GenerateReportRequest {
    private Long communityId;
    private String reportType;   // AGM_FINANCIAL, GST_SUMMARY, TDS_SUMMARY, MAINTENANCE_FUND, AUDIT_EXPORT
    private String period;       // e.g. "2025-11", "2025-Q3", "2025"
    private String fromDate;     // ISO date, for AUDIT_EXPORT
    private String toDate;
}
