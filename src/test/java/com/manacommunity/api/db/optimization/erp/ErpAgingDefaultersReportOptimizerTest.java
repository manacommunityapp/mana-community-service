package com.manacommunity.api.db.optimization.erp;

import com.manacommunity.api.db.optimization.erp.model.FlatAgingSummaryDto;
import com.manacommunity.api.db.optimization.erp.model.InvoiceRecordDto;
import com.manacommunity.api.db.optimization.erp.service.ErpAgingDefaultersReportOptimizerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErpAgingDefaultersReportOptimizerTest {

    private ErpAgingDefaultersReportOptimizerImpl optimizer;
    private final Long communityId = 7002L;

    @BeforeEach
    void setUp() {
        optimizer = new ErpAgingDefaultersReportOptimizerImpl();
    }

    @Test
    @DisplayName("Should categorize multi-year debts into accurate aging buckets and update incrementally on payment")
    void testAgingDefaultersSummaryAndIncrementalPayment() {
        LocalDate today = LocalDate.now();

        // Flat 101 has 3 overdue invoices:
        // Invoice 1: Due 15 days ago (Current 0-30 days) -> 5,000
        // Invoice 2: Due 45 days ago (31-60 days) -> 3,000
        // Invoice 3: Due 120 days ago (90+ days) -> 10,000
        InvoiceRecordDto inv1 = InvoiceRecordDto.builder()
                .invoiceId(1L).communityId(communityId).flatId(101L).unitNumber("A-101").residentName("Karan Johar")
                .dueDate(today.minusDays(15)).totalAmount(BigDecimal.valueOf(5000)).status("PENDING").build();

        InvoiceRecordDto inv2 = InvoiceRecordDto.builder()
                .invoiceId(2L).communityId(communityId).flatId(101L).unitNumber("A-101").residentName("Karan Johar")
                .dueDate(today.minusDays(45)).totalAmount(BigDecimal.valueOf(3000)).status("PENDING").build();

        InvoiceRecordDto inv3 = InvoiceRecordDto.builder()
                .invoiceId(3L).communityId(communityId).flatId(101L).unitNumber("A-101").residentName("Karan Johar")
                .dueDate(today.minusDays(120)).totalAmount(BigDecimal.valueOf(10000)).status("PENDING").build();

        optimizer.seedInvoices(communityId, Arrays.asList(inv1, inv2, inv3));

        List<FlatAgingSummaryDto> report = optimizer.getAgingDefaultersReportMaterialized(communityId);

        assertNotNull(report);
        assertEquals(1, report.size());
        FlatAgingSummaryDto flatSummary = report.get(0);
        assertEquals(new BigDecimal("5000"), flatSummary.getCurrentDue());
        assertEquals(new BigDecimal("3000"), flatSummary.getDue30Days());
        assertEquals(BigDecimal.ZERO, flatSummary.getDue60Days());
        assertEquals(new BigDecimal("10000"), flatSummary.getDue90PlusDays());
        assertEquals(new BigDecimal("18000"), flatSummary.getTotalOutstanding());

        // Now resident pays 11,000 (Clears 10,000 90+ day invoice + 1,000 partial on 45-day invoice)
        optimizer.recordPayment(communityId, 101L, BigDecimal.valueOf(11000), LocalDateTime.now());

        List<FlatAgingSummaryDto> updatedReport = optimizer.getAgingDefaultersReportMaterialized(communityId);
        FlatAgingSummaryDto updatedSummary = updatedReport.get(0);

        assertEquals(new BigDecimal("5000"), updatedSummary.getCurrentDue());
        assertEquals(new BigDecimal("2000"), updatedSummary.getDue30Days()); // 3,000 - 1,000 = 2,000
        assertEquals(BigDecimal.ZERO, updatedSummary.getDue90PlusDays());    // 10,000 fully paid
        assertEquals(new BigDecimal("7000"), updatedSummary.getTotalOutstanding());
        assertNotNull(updatedSummary.getLastPaymentDate());

        // Verify EXPLAIN ANALYZE comparison string
        String explain = optimizer.explainAnalyzeReportComparison();
        assertTrue(explain.contains("14,215ms"));
        assertTrue(explain.contains("4.2ms"));
    }
}
