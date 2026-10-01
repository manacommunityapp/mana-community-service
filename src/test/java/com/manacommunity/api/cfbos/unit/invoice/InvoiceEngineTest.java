package com.manacommunity.api.cfbos.unit.invoice;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.billing.entity.BillingRun;
import com.manacommunity.api.cfbos.billing.entity.BillingRunLine;
import com.manacommunity.api.cfbos.billing.entity.ChargeType;
import com.manacommunity.api.cfbos.billing.repository.BillingRunRepository;
import com.manacommunity.api.cfbos.invoice.engine.InvoiceEngine;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceEngineTest {

    @Mock private CfbosInvoiceRepository invoiceRepository;
    @Mock private BillingRunRepository billingRunRepository;
    @Mock private DocumentSequenceService documentSequenceService;
    @Mock private TaxEngine taxEngine;
    @Mock private AccountingEngine accountingEngine;

    private InvoiceEngine invoiceEngine;

    @BeforeEach
    void setUp() {
        invoiceEngine = new InvoiceEngine(
                invoiceRepository, billingRunRepository, documentSequenceService, taxEngine, accountingEngine
        );
    }

    @Test
    @DisplayName("Generate invoices from billing run creates invoices with tax breakdown")
    void generateInvoicesFromBillingRun() {
        ChargeType ct = ChargeType.builder().id(1L).code("MAINT").name("Maintenance").defaultHsnSacCode("9995").build();
        BillingRun run = BillingRun.builder()
                .id(1L)
                .runNumber("BR-2026-0001")
                .billingPeriodStart(LocalDate.of(2026, 4, 1))
                .billingPeriodEnd(LocalDate.of(2026, 4, 30))
                .build();

        BillingRunLine line = BillingRunLine.builder()
                .id(10L)
                .billingRun(run)
                .propertyId(100L)
                .residentId(500L)
                .chargeType(ct)
                .description("Monthly Maintenance")
                .amount(new BigDecimal("4000.00"))
                .taxAmount(new BigDecimal("720.00"))
                .totalAmount(new BigDecimal("4720.00"))
                .build();

        run.setLines(List.of(line));

        when(billingRunRepository.findById(1L)).thenReturn(Optional.of(run));
        when(documentSequenceService.nextNumber(any(), anyString())).thenReturn("INV-2026-000001");
        when(taxEngine.calculateGst(any(), any(), any()))
                .thenReturn(GstCalculationResult.builder()
                        .taxableAmount(new BigDecimal("4000.00"))
                        .cgstRate(new BigDecimal("9.00"))
                        .cgstAmount(new BigDecimal("360.00"))
                        .sgstRate(new BigDecimal("9.00"))
                        .sgstAmount(new BigDecimal("360.00"))
                        .totalTax(new BigDecimal("720.00"))
                        .build());

        when(invoiceRepository.save(any(CfbosInvoice.class))).thenAnswer(i -> i.getArgument(0));

        List<CfbosInvoice> invoices = invoiceEngine.generateInvoicesFromBillingRun(1L);

        assertThat(invoices).hasSize(1);
        CfbosInvoice inv = invoices.get(0);
        assertThat(inv.getInvoiceNumber()).isEqualTo("INV-2026-000001");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(inv.getSubtotal()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(inv.getTotalTax()).isEqualByComparingTo(new BigDecimal("720.00"));
        assertThat(inv.getTotalAmount()).isEqualByComparingTo(new BigDecimal("4720.00"));
        assertThat(inv.getBalanceDue()).isEqualByComparingTo(new BigDecimal("4720.00"));
    }
}
