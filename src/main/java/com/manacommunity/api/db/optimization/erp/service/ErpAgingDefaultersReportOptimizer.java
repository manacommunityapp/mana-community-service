package com.manacommunity.api.db.optimization.erp.service;

import com.manacommunity.api.db.optimization.erp.model.FlatAgingSummaryDto;
import com.manacommunity.api.db.optimization.erp.model.InvoiceRecordDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ErpAgingDefaultersReportOptimizer {

    void seedInvoices(Long communityId, List<InvoiceRecordDto> invoices);

    void recordPayment(Long communityId, Long flatId, BigDecimal paymentAmount, LocalDateTime paymentDate);

    List<FlatAgingSummaryDto> getAgingDefaultersReportMaterialized(Long communityId);

    void rebuildMaterializedSummary(Long communityId, LocalDate asOfDate);

    String explainAnalyzeReportComparison();
}
