package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoiceLine;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceType;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.transaction.model.TransactionIntent;
import com.manacommunity.api.transaction.model.TransactionLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionInvoicePillar {

    private final CfbosInvoiceRepository invoiceRepository;
    private final DocumentSequenceService documentSequenceService;

    @Transactional
    public CfbosInvoice generateInvoice(TransactionIntent intent, String transactionNumber) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());

        String invoiceNumber;
        try {
            invoiceNumber = documentSequenceService.nextNumber(DocumentType.INVOICE, year);
        } catch (Exception e) {
            invoiceNumber = "INV-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        BigDecimal subtotal = intent.getAmount();
        BigDecimal tax = intent.getTaxAmount() != null ? intent.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal cgst = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = tax.subtract(cgst);
        BigDecimal total = subtotal.add(tax).subtract(intent.getDiscountAmount() != null ? intent.getDiscountAmount() : BigDecimal.ZERO);

        CfbosInvoice invoice = CfbosInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(now)
                .dueDate(now.plusDays(7))
                .propertyId(intent.getPropertyId() != null ? intent.getPropertyId() : 0L)
                .residentId(intent.getPayerId())
                .invoiceType(InvoiceType.REGULAR)
                .status(InvoiceStatus.PAID)
                .subtotal(subtotal)
                .discountAmount(intent.getDiscountAmount() != null ? intent.getDiscountAmount() : BigDecimal.ZERO)
                .taxableAmount(subtotal)
                .cgstAmount(cgst)
                .sgstAmount(sgst)
                .igstAmount(BigDecimal.ZERO)
                .totalTax(tax)
                .totalAmount(total)
                .amountPaid(total)
                .balanceDue(BigDecimal.ZERO)
                .isGstInvoice(tax.compareTo(BigDecimal.ZERO) > 0)
                .notes("Transaction Core invoice for " + intent.getDomain() + " [" + transactionNumber + "]")
                .lines(new ArrayList<>())
                .build();

        if (intent.getLineItems() != null && !intent.getLineItems().isEmpty()) {
            int order = 1;
            for (TransactionLineItem item : intent.getLineItems()) {
                BigDecimal qty = item.getQuantity() != null ? BigDecimal.valueOf(item.getQuantity()) : BigDecimal.ONE;
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : item.getTotalPrice();
                BigDecimal itemTax = item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO;

                CfbosInvoiceLine line = CfbosInvoiceLine.builder()
                        .invoice(invoice)
                        .description(item.getDescription() != null ? item.getDescription() : intent.getDomain().name())
                        .hsnSacCode(item.getHsnSacCode() != null ? item.getHsnSacCode() : "9983")
                        .quantity(qty)
                        .rate(unitPrice)
                        .amount(item.getTotalPrice() != null ? item.getTotalPrice() : unitPrice.multiply(qty))
                        .taxableAmount(item.getTotalPrice() != null ? item.getTotalPrice() : unitPrice.multiply(qty))
                        .cgstAmount(itemTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                        .sgstAmount(itemTax.subtract(itemTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)))
                        .totalAmount((item.getTotalPrice() != null ? item.getTotalPrice() : unitPrice.multiply(qty)).add(itemTax))
                        .lineOrder(order++)
                        .build();
                invoice.getLines().add(line);
            }
        } else {
            CfbosInvoiceLine line = CfbosInvoiceLine.builder()
                    .invoice(invoice)
                    .description(intent.getNarration() != null ? intent.getNarration() : (intent.getDomain().name() + " Service"))
                    .hsnSacCode("9983")
                    .quantity(BigDecimal.ONE)
                    .rate(subtotal)
                    .amount(subtotal)
                    .taxableAmount(subtotal)
                    .cgstAmount(cgst)
                    .sgstAmount(sgst)
                    .totalAmount(total)
                    .lineOrder(1)
                    .build();
            invoice.getLines().add(line);
        }

        return invoiceRepository.save(invoice);
    }
}
