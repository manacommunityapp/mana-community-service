package com.manacommunity.api.finance.compliance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReportData(
        String title,
        String period,
        List<Map<String, Object>> rows,
        Map<String, Object> summary,
        BigDecimal totalAmount,
        int rowCount
) {}
