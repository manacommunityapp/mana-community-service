package com.manacommunity.api.cfbos.invoice.engine;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.billing.entity.BillingRun;
import com.manacommunity.api.cfbos.billing.entity.BillingRunLine;
import com.manacommunity.api.cfbos.billing.enums.BillingRunStatus;
import com.manacommunity.api.cfbos.billing.repository.BillingRunRepository;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoiceLine;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoiceTaxLine;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceType;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceEngine {

    private final CfbosInvoiceRepository invoiceRepository;
    private final BillingRunRepository billingRunRepository;
    private final DocumentSequenceService documentSequenceService;
    private final TaxEngine taxEngine;
    private final AccountingEngine accountingEngine;

    private static final BigDecimal DEFAULT_CGST_RATE = new BigDecimal("9.00");
    private static final BigDecimal DEFAULT_SGST_RATE = new BigDecimal("9.00");

    @Transactional
    public List<CfbosInvoice> generateInvoicesFromBillingRun(Long billingRunId) {
        BillingRun run = billingRunRepository.findById(billingRunId)
                .orElseThrow(() -> new CfbosException("BillingRun not found: " + billingRunId));

        if (run.getStatus() == BillingRunStatus.INVOICED) {
            throw new CfbosException("Billing run has already been invoiced: " + run.getRunNumber());
        }

        Map<Long, List<BillingRunLine>> propertyLinesMap = run.getLines().stream()
                .collect(Collectors.groupingBy(BillingRunLine::getPropertyId));

        List<CfbosInvoice> invoices = new ArrayList<>();
        String fiscalYear = String.valueOf(run.getBillingPeriodStart().getYear());

        for (Map.Entry<Long, List<BillingRunLine>> propEntry : propertyLinesMap.entrySet()) {
            Long propertyId = propEntry.getKey();
            List<BillingRunLine> lines = propEntry.getValue();
            if (lines.isEmpty()) continue;

            Long residentId = lines.get(0).getResidentId();
            String invoiceNumber = documentSequenceService.nextNumber(DocumentType.INVOICE, fiscalYear);

            LocalDate invDate = LocalDate.now();
            int dueOffset = run.getBillingSchedule() != null ? run.getBillingSchedule().getDueDayOffset() : 15;
            LocalDate dueDate = invDate.plusDays(dueOffset);

            CfbosInvoice invoice = CfbosInvoice.builder()
                    .invoiceNumber(invoiceNumber)
                    .invoiceDate(invDate)
                    .dueDate(dueDate)
                    .propertyId(propertyId)
                    .residentId(residentId)
                    .billingRunId(run.getId())
                    .invoiceType(InvoiceType.MAINTENANCE)
                    .status(InvoiceStatus.ISSUED)
                    .billingPeriodStart(run.getBillingPeriodStart())
                    .billingPeriodEnd(run.getBillingPeriodEnd())
                    .notes("Generated from Billing Run: " + run.getRunNumber())
                    .terms("Payment due by " + dueDate)
                    .isGstInvoice(true)
                    .lines(new ArrayList<>())
                    .taxLines(new ArrayList<>())
                    .build();

            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal totalCgst = BigDecimal.ZERO;
            BigDecimal totalSgst = BigDecimal.ZERO;

            for (int i = 0; i < lines.size(); i++) {
                BillingRunLine runLine = lines.get(i);
                BigDecimal amount = runLine.getAmount();
                subtotal = subtotal.add(amount);

                String hsnSac = (runLine.getChargeType() != null && runLine.getChargeType().getDefaultHsnSacCode() != null)
                        ? runLine.getChargeType().getDefaultHsnSacCode() : "9995";

                GstCalculationResult gst = taxEngine.calculateGst(amount, DEFAULT_CGST_RATE, DEFAULT_SGST_RATE);
                totalCgst = totalCgst.add(gst.getCgstAmount());
                totalSgst = totalSgst.add(gst.getSgstAmount());

                CfbosInvoiceLine line = CfbosInvoiceLine.builder()
                        .invoice(invoice)
                        .chargeTypeId(runLine.getChargeType() != null ? runLine.getChargeType().getId() : null)
                        .description(runLine.getDescription())
                        .hsnSacCode(hsnSac)
                        .quantity(runLine.getQuantity())
                        .rate(runLine.getRate())
                        .amount(amount)
                        .taxableAmount(amount)
                        .cgstRate(gst.getCgstRate())
                        .cgstAmount(gst.getCgstAmount())
                        .sgstRate(gst.getSgstRate())
                        .sgstAmount(gst.getSgstAmount())
                        .igstRate(BigDecimal.ZERO)
                        .igstAmount(BigDecimal.ZERO)
                        .totalAmount(amount.add(gst.getTotalTax()))
                        .lineOrder(i + 1)
                        .billingRunLineId(runLine.getId())
                        .build();

                invoice.getLines().add(line);
            }

            BigDecimal totalTax = totalCgst.add(totalSgst);
            BigDecimal totalAmount = subtotal.add(totalTax);

            invoice.setSubtotal(subtotal);
            invoice.setTaxableAmount(subtotal);
            invoice.setCgstAmount(totalCgst);
            invoice.setSgstAmount(totalSgst);
            invoice.setIgstAmount(BigDecimal.ZERO);
            invoice.setTotalTax(totalTax);
            invoice.setTotalAmount(totalAmount);
            invoice.setBalanceDue(totalAmount);

            if (totalCgst.compareTo(BigDecimal.ZERO) > 0) {
                invoice.getTaxLines().add(CfbosInvoiceTaxLine.builder()
                        .invoice(invoice)
                        .taxType("CGST")
                        .taxRate(DEFAULT_CGST_RATE)
                        .taxableAmount(subtotal)
                        .taxAmount(totalCgst)
                        .build());
            }
            if (totalSgst.compareTo(BigDecimal.ZERO) > 0) {
                invoice.getTaxLines().add(CfbosInvoiceTaxLine.builder()
                        .invoice(invoice)
                        .taxType("SGST")
                        .taxRate(DEFAULT_SGST_RATE)
                        .taxableAmount(subtotal)
                        .taxAmount(totalSgst)
                        .build());
            }

            try {
                List<JournalEntryRequest.LineRequest> jLines = new ArrayList<>();
                jLines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("1200")
                        .debitAmount(totalAmount)
                        .narration("Invoice " + invoiceNumber)
                        .build());
                jLines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("4100")
                        .creditAmount(subtotal)
                        .narration("Maintenance revenue for " + invoiceNumber)
                        .build());
                if (totalCgst.compareTo(BigDecimal.ZERO) > 0) {
                    jLines.add(JournalEntryRequest.LineRequest.builder()
                            .accountCode("2110")
                            .creditAmount(totalCgst)
                            .narration("Output CGST on " + invoiceNumber)
                            .build());
                }
                if (totalSgst.compareTo(BigDecimal.ZERO) > 0) {
                    jLines.add(JournalEntryRequest.LineRequest.builder()
                            .accountCode("2120")
                            .creditAmount(totalSgst)
                            .narration("Output SGST on " + invoiceNumber)
                            .build());
                }

                JournalEntry jEntry = accountingEngine.createAndPostJournalEntry(
                        JournalEntryRequest.builder()
                                .entryDate(invDate)
                                .sourceModule(SourceModule.BILLING)
                                .sourceDocumentType("INVOICE")
                                .narration("Invoice issued: " + invoiceNumber)
                                .lines(jLines)
                                .build()
                );
                invoice.setJournalEntryId(jEntry.getId());
            } catch (Exception ignored) {}

            invoices.add(invoiceRepository.save(invoice));
        }

        run.setStatus(BillingRunStatus.INVOICED);
        billingRunRepository.save(run);

        return invoices;
    }
}
