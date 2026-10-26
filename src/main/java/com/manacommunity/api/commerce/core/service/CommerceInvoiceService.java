package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.finance.dto.FinanceDocumentDto;
import com.manacommunity.api.finance.entity.FinanceDocument;
import com.manacommunity.api.finance.repository.FinanceDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceInvoiceService {

    private final FinanceDocumentRepository financeDocumentRepository;
    private final CommerceOrderRepository orderRepository;

    /**
     * Generates a GST invoice (FinanceDocument type=INVOICE) for a completed commerce
     * order and stores the document ID back on the order. Idempotent — calling it twice
     * for the same order returns the existing document without creating a duplicate.
     */
    @Transactional
    public FinanceDocumentDto generateInvoiceForOrder(CommerceOrder order) {
        if (order.getInvoiceDocumentId() != null) {
            return financeDocumentRepository.findById(order.getInvoiceDocumentId())
                    .map(this::toDto)
                    .orElseGet(() -> createAndSave(order));
        }
        return createAndSave(order);
    }

    @Transactional(readOnly = true)
    public Optional<FinanceDocumentDto> findInvoiceForOrder(CommerceOrder order) {
        if (order.getInvoiceDocumentId() == null) return Optional.empty();
        return financeDocumentRepository.findById(order.getInvoiceDocumentId()).map(this::toDto);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private FinanceDocumentDto createAndSave(CommerceOrder order) {
        List<FinanceDocumentDto.Line> lines = order.getItems().stream()
                .map(this::toLine)
                .toList();

        BigDecimal subtotal = order.getSubtotalAmount() != null ? order.getSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal tax = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal delivery = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

        String buyerName = order.getBuyer() != null ? order.getBuyer().getFullName() : "Resident";
        Long buyerId = order.getBuyer() != null ? order.getBuyer().getId() : null;
        String vendorNote = "Commerce Order: " + order.getOrderNumber()
                + (order.getVendorName() != null ? " · Vendor: " + order.getVendorName() : "");

        var doc = com.manacommunity.api.finance.entity.FinanceDocument.builder()
                .type(FinanceDocument.Type.INVOICE)
                .status("Paid")
                .customerId(buyerId)
                .customerName(buyerName)
                .docDate(LocalDate.now())
                .currency("INR")
                .taxInclusive(true)
                .subtotal(subtotal)
                .discount(discount)
                .tax(tax)
                .otherCharges(delivery.compareTo(BigDecimal.ZERO) > 0 ? delivery : null)
                .grandTotal(grandTotal)
                .notes(vendorNote)
                .build();

        // Manually set lines since builder uses a separate add pattern
        for (FinanceDocumentDto.Line l : lines) {
            var line = com.manacommunity.api.finance.entity.FinanceDocumentLine.builder()
                    .document(doc)
                    .item(l.getItem())
                    .description(l.getDescription())
                    .qty(l.getQty() != null ? l.getQty() : 1)
                    .cost(l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                    .disc(BigDecimal.ZERO)
                    .tax(BigDecimal.ZERO)
                    .lineTotal(l.getLineTotal() != null ? l.getLineTotal() : BigDecimal.ZERO)
                    .build();
            doc.getLines().add(line);
        }

        var saved = financeDocumentRepository.save(doc);

        // Auto-generate code: INV/{id}
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode("INV/" + String.format("%04d", saved.getId()));
            saved = financeDocumentRepository.save(saved);
        }

        // Link back to the order
        order.setInvoiceDocumentId(saved.getId());
        orderRepository.save(order);

        log.info("Commerce invoice generated: {} for order {}", saved.getCode(), order.getOrderNumber());
        return toDto(saved);
    }

    private FinanceDocumentDto.Line toLine(CommerceOrderItem item) {
        return FinanceDocumentDto.Line.builder()
                .item(item.getTitle())
                .description(item.getSku() != null ? "SKU: " + item.getSku() : null)
                .qty(item.getQuantity())
                .cost(item.getUnitPrice())
                .disc(BigDecimal.ZERO)
                .tax(item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO)
                .lineTotal(item.getTotalPrice())
                .build();
    }

    private FinanceDocumentDto toDto(FinanceDocument d) {
        List<FinanceDocumentDto.Line> lines = d.getLines().stream()
                .map(l -> FinanceDocumentDto.Line.builder()
                        .item(l.getItem())
                        .description(l.getDescription())
                        .qty(l.getQty())
                        .cost(l.getCost())
                        .disc(l.getDisc())
                        .tax(l.getTax())
                        .lineTotal(l.getLineTotal())
                        .build())
                .toList();
        return FinanceDocumentDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .type(d.getType())
                .status(d.getStatus())
                .customerId(d.getCustomerId())
                .customerName(d.getCustomerName())
                .docDate(d.getDocDate())
                .currency(d.getCurrency())
                .taxInclusive(d.isTaxInclusive())
                .subtotal(d.getSubtotal())
                .discount(d.getDiscount())
                .tax(d.getTax())
                .otherCharges(d.getOtherCharges())
                .grandTotal(d.getGrandTotal())
                .notes(d.getNotes())
                .items(lines)
                .build();
    }
}
