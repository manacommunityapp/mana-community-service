package com.manacommunity.api.finance.service;

import com.manacommunity.api.billing.event.InvoiceIssuedEvent;
import com.manacommunity.api.billing.event.InvoicePaymentReceivedEvent;
import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.dto.FinanceInvoiceDto;
import com.manacommunity.api.finance.entity.FinanceInvoice;
import com.manacommunity.api.finance.entity.FinanceLineItem;
import com.manacommunity.api.finance.repository.FinanceInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for customer invoices (Income -> Invoices menu).
 *
 * <p>Every read and write is scoped to the caller's community. The community comes from
 * {@link TenantContext}, which {@code TenantFilter} now populates from the authenticated
 * principal rather than the client-supplied {@code X-Community-Id} header.
 */
@Service
@RequiredArgsConstructor
public class FinanceInvoiceService {

    private static final String CODE_PREFIX = "INV";
    private static final String STATUS_PAID = "Paid";

    private final FinanceInvoiceRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<FinanceInvoiceDto> getAll(String status) {
        Long communityId = requireCommunityId();
        List<FinanceInvoice> docs = (status != null && !status.isBlank())
                ? repo.findByCommunityIdAndStatusOrderByDocDateDesc(communityId, status)
                : repo.findByCommunityIdOrderByDocDateDesc(communityId);
        return docs.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FinanceInvoiceDto get(Long id) {
        return toDto(require(id, requireCommunityId()));
    }

    @Transactional
    public FinanceInvoiceDto create(FinanceInvoiceDto dto) {
        Long communityId = requireCommunityId();
        FinanceInvoice doc = FinanceInvoice.builder()
                .communityId(communityId)
                .code(dto.getCode())
                .status(dto.getStatus())
                .customerId(dto.getCustomerId())
                .customerName(dto.getCustomerName())
                .docDate(dto.getDocDate() != null ? dto.getDocDate() : LocalDate.now())
                .dueDate(dto.getDueDate())
                .notes(dto.getNotes())
                .terms(dto.getTerms())
                .taxInclusive(dto.isTaxInclusive())
                .currency(dto.getCurrency())
                .subtotal(dto.getSubtotal())
                .discount(dto.getDiscount())
                .tax(dto.getTax())
                .otherCharges(dto.getOtherCharges())
                .grandTotal(dto.getGrandTotal())
                .lines(new ArrayList<>())
                .build();
        applyLines(doc, dto);

        FinanceInvoice saved = repo.save(doc);
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode(CODE_PREFIX + "/" + String.format("%03d", saved.getId()));
            saved = repo.save(saved);
        }

        // Publish invoice-issued event so the ledger listener can create journal entries.
        // The listener is an AFTER_COMMIT transactional listener, so no ledger row exists
        // unless this transaction actually commits.
        BigDecimal invoiceAmount = saved.getGrandTotal() != null ? saved.getGrandTotal() : BigDecimal.ZERO;
        if (invoiceAmount.compareTo(BigDecimal.ZERO) > 0) {
            eventPublisher.publishEvent(new InvoiceIssuedEvent(
                    saved.getId(),
                    communityId,
                    saved.getCustomerId(),
                    invoiceAmount,
                    saved.getCode(),
                    saved.getDueDate()
            ));
        }

        return toDto(saved);
    }

    @Transactional
    public FinanceInvoiceDto update(Long id, FinanceInvoiceDto dto) {
        Long communityId = requireCommunityId();
        FinanceInvoice doc = require(id, communityId);

        // A payment is recorded at most once per invoice. The previous check compared the
        // new status only against the immediately previous one, so a Paid -> Pending -> Paid
        // cycle re-published InvoicePaymentReceivedEvent and double-posted DR Cash / CR AR.
        boolean paymentAlreadyRecorded = doc.getPaymentRecordedAt() != null;

        doc.setStatus(dto.getStatus());
        doc.setCustomerId(dto.getCustomerId());
        doc.setCustomerName(dto.getCustomerName());
        if (dto.getDocDate() != null) doc.setDocDate(dto.getDocDate());
        doc.setDueDate(dto.getDueDate());
        doc.setNotes(dto.getNotes());
        doc.setTerms(dto.getTerms());
        doc.setTaxInclusive(dto.isTaxInclusive());
        doc.setCurrency(dto.getCurrency());
        doc.setSubtotal(dto.getSubtotal());
        doc.setDiscount(dto.getDiscount());
        doc.setTax(dto.getTax());
        doc.setOtherCharges(dto.getOtherCharges());
        doc.setGrandTotal(dto.getGrandTotal());
        if (dto.getCode() != null && !dto.getCode().isBlank()) doc.setCode(dto.getCode());
        doc.getLines().clear();
        applyLines(doc, dto);

        BigDecimal paidAmount = doc.getGrandTotal() != null ? doc.getGrandTotal() : BigDecimal.ZERO;
        boolean firstTransitionToPaid = STATUS_PAID.equalsIgnoreCase(dto.getStatus())
                && !paymentAlreadyRecorded
                && paidAmount.compareTo(BigDecimal.ZERO) > 0;
        if (firstTransitionToPaid) {
            doc.setPaymentRecordedAt(LocalDateTime.now());
        }

        FinanceInvoice saved = repo.save(doc);

        // Publish only on the first genuine transition to "Paid".
        if (firstTransitionToPaid) {
            eventPublisher.publishEvent(new InvoicePaymentReceivedEvent(
                    saved.getId(),
                    communityId,
                    saved.getCustomerId(),
                    paidAmount,
                    saved.getCode(),
                    saved.getCode()   // use invoice code as payment reference
            ));
        }

        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long communityId = requireCommunityId();
        if (!repo.existsByIdAndCommunityId(id, communityId)) {
            throw new ResourceNotFoundException("FinanceInvoice", id);
        }
        repo.deleteById(id);
    }

    // -- Tenant resolution ----------------------------------------------------

    /**
     * Resolves the caller's community, failing closed. Without a community we must not
     * fall back to an unscoped query (that is the cross-tenant leak this fixes) and must
     * not create an invoice whose journal entry cannot be attributed to any set of books.
     */
    private Long requireCommunityId() {
        Long communityId = TenantContext.getCommunityId();
        if (communityId == null) {
            throw new AccessDeniedException("No community in context - finance invoices are "
                    + "tenant-scoped and cannot be read or written without an authenticated community.");
        }
        return communityId;
    }

    // -- Mapping --------------------------------------------------------------

    private FinanceInvoice require(Long id, Long communityId) {
        return repo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FinanceInvoice", id));
    }

    private void applyLines(FinanceInvoice doc, FinanceInvoiceDto dto) {
        if (dto.getItems() == null) return;
        for (FinanceInvoiceDto.Line l : dto.getItems()) {
            boolean empty = (l.getItem() == null || l.getItem().isBlank())
                    && (l.getQty() == null || l.getQty() == 0);
            if (empty) continue;
            doc.getLines().add(FinanceLineItem.builder()
                    .item(l.getItem())
                    .description(l.getDescription())
                    .qty(l.getQty() != null ? l.getQty() : 0)
                    .cost(l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                    .disc(l.getDisc() != null ? l.getDisc() : BigDecimal.ZERO)
                    .tax(l.getTax() != null ? l.getTax() : BigDecimal.ZERO)
                    .lineTotal(l.getLineTotal() != null ? l.getLineTotal() : BigDecimal.ZERO)
                    .build());
        }
    }

    private FinanceInvoiceDto toDto(FinanceInvoice d) {
        return FinanceInvoiceDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .status(d.getStatus())
                .customerId(d.getCustomerId())
                .customerName(d.getCustomerName())
                .docDate(d.getDocDate())
                .dueDate(d.getDueDate())
                .notes(d.getNotes())
                .terms(d.getTerms())
                .taxInclusive(d.isTaxInclusive())
                .currency(d.getCurrency())
                .subtotal(d.getSubtotal())
                .discount(d.getDiscount())
                .tax(d.getTax())
                .otherCharges(d.getOtherCharges())
                .grandTotal(d.getGrandTotal())
                .items(d.getLines().stream().map(l -> FinanceInvoiceDto.Line.builder()
                        .item(l.getItem())
                        .description(l.getDescription())
                        .qty(l.getQty())
                        .cost(l.getCost())
                        .disc(l.getDisc())
                        .tax(l.getTax())
                        .lineTotal(l.getLineTotal())
                        .build()).toList())
                .build();
    }
}
