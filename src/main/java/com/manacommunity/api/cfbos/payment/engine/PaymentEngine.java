package com.manacommunity.api.cfbos.payment.engine;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.payment.dto.RecordPaymentRequest;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosPaymentLine;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.cfbos.payment.repository.CfbosReceiptRepository;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentEngine {

    private final CfbosPaymentRepository paymentRepository;
    private final CfbosReceiptRepository receiptRepository;
    private final CfbosInvoiceRepository invoiceRepository;
    private final DocumentSequenceService documentSequenceService;
    private final AccountingEngine accountingEngine;

    @Transactional
    public CfbosPayment processPayment(RecordPaymentRequest request) {
        LocalDate pDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();
        String fiscalYear = String.valueOf(pDate.getYear());

        String paymentNumber = documentSequenceService.nextNumber(DocumentType.RECEIPT, fiscalYear);
        String receiptNumber = "RCP-" + paymentNumber.replace("REC-", "").replace("DOC-", "");

        CfbosPayment payment = CfbosPayment.builder()
                .paymentNumber(paymentNumber)
                .paymentDate(pDate)
                .residentId(request.getResidentId())
                .propertyId(request.getPropertyId() != null ? request.getPropertyId() : 0L)
                .paymentMethod(request.getPaymentMethod())
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "ONLINE")
                .amount(request.getAmount())
                .appliedAmount(BigDecimal.ZERO)
                .unappliedAmount(request.getAmount())
                .gatewayReference(request.getGatewayReference())
                .remarks(request.getRemarks())
                .status(PaymentStatus.SUCCESS)
                .lines(new ArrayList<>())
                .build();

        // Find candidate invoices
        List<CfbosInvoice> openInvoices = invoiceRepository.findByResidentId(request.getResidentId()).stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.ISSUED || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID || inv.getStatus() == InvoiceStatus.OVERDUE)
                .sorted(Comparator.comparing(CfbosInvoice::getDueDate))
                .collect(Collectors.toList());

        BigDecimal remainingPayment = request.getAmount();
        BigDecimal totalApplied = BigDecimal.ZERO;

        for (CfbosInvoice inv : openInvoices) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;

            if (request.getSpecificInvoiceIds() != null && !request.getSpecificInvoiceIds().isEmpty() &&
                    !request.getSpecificInvoiceIds().contains(inv.getId())) {
                continue;
            }

            BigDecimal balance = inv.getBalanceDue();
            BigDecimal allocation = remainingPayment.min(balance);

            inv.setAmountPaid(inv.getAmountPaid().add(allocation));
            inv.setBalanceDue(inv.getBalanceDue().subtract(allocation));

            if (inv.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
                inv.setStatus(InvoiceStatus.PAID);
            } else {
                inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }

            invoiceRepository.save(inv);

            CfbosPaymentLine line = CfbosPaymentLine.builder()
                    .payment(payment)
                    .invoiceId(inv.getId())
                    .allocatedAmount(allocation)
                    .allocationDate(pDate)
                    .build();

            payment.getLines().add(line);
            totalApplied = totalApplied.add(allocation);
            remainingPayment = remainingPayment.subtract(allocation);
        }

        payment.setAppliedAmount(totalApplied);
        payment.setUnappliedAmount(remainingPayment);

        payment = paymentRepository.save(payment);

        // Generate Receipt
        CfbosReceipt receipt = CfbosReceipt.builder()
                .receiptNumber(receiptNumber)
                .receiptDate(pDate)
                .paymentId(payment.getId())
                .residentId(request.getResidentId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .narration("Receipt for payment " + paymentNumber)
                .build();
        receipt = receiptRepository.save(receipt);
        payment.setReceiptId(receipt.getId());

        // Accounting entry
        try {
            List<JournalEntryRequest.LineRequest> jLines = new ArrayList<>();
            // DR Bank Account 1110
            jLines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("1110")
                    .debitAmount(request.getAmount())
                    .narration("Collection via " + request.getPaymentMethod() + " (" + paymentNumber + ")")
                    .build());
            // CR Accounts Receivable 1200
            jLines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("1200")
                    .creditAmount(request.getAmount())
                    .narration("Payment credited from Resident " + request.getResidentId())
                    .build());

            JournalEntry entry = accountingEngine.createAndPostJournalEntry(
                    JournalEntryRequest.builder()
                            .entryDate(pDate)
                            .sourceModule(SourceModule.PAYMENT)
                            .sourceDocumentType("PAYMENT")
                            .narration("Payment received: " + paymentNumber)
                            .lines(jLines)
                            .build()
            );
            payment.setJournalEntryId(entry.getId());
            receipt.setJournalEntryId(entry.getId());
            receiptRepository.save(receipt);
        } catch (Exception ignored) {}

        return paymentRepository.save(payment);
    }
}
