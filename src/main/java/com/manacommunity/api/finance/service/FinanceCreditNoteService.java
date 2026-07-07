package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.dto.FinanceCreditNoteDto;
import com.manacommunity.api.finance.entity.FinanceCreditNote;
import com.manacommunity.api.finance.entity.FinanceLineItem;
import com.manacommunity.api.finance.repository.FinanceCreditNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Business logic for customer credit notes (Income → Credit Notes menu). */
@Service
@RequiredArgsConstructor
public class FinanceCreditNoteService {

    private static final String CODE_PREFIX = "CN";

    private final FinanceCreditNoteRepository repo;

    @Transactional(readOnly = true)
    public List<FinanceCreditNoteDto> getAll(String status) {
        List<FinanceCreditNote> docs = (status != null && !status.isBlank())
                ? repo.findByStatusOrderByDocDateDesc(status)
                : repo.findAllByOrderByDocDateDesc();
        return docs.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FinanceCreditNoteDto get(Long id) {
        return toDto(require(id));
    }

    @Transactional
    public FinanceCreditNoteDto create(FinanceCreditNoteDto dto) {
        FinanceCreditNote doc = FinanceCreditNote.builder()
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

        FinanceCreditNote saved = repo.save(doc);
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode(CODE_PREFIX + "/" + String.format("%03d", saved.getId()));
            saved = repo.save(saved);
        }
        return toDto(saved);
    }

    @Transactional
    public FinanceCreditNoteDto update(Long id, FinanceCreditNoteDto dto) {
        FinanceCreditNote doc = require(id);
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
        return toDto(repo.save(doc));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("FinanceCreditNote", id);
        repo.deleteById(id);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private FinanceCreditNote require(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("FinanceCreditNote", id));
    }

    private void applyLines(FinanceCreditNote doc, FinanceCreditNoteDto dto) {
        if (dto.getItems() == null) return;
        for (FinanceCreditNoteDto.Line l : dto.getItems()) {
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

    private FinanceCreditNoteDto toDto(FinanceCreditNote d) {
        return FinanceCreditNoteDto.builder()
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
                .items(d.getLines().stream().map(l -> FinanceCreditNoteDto.Line.builder()
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
