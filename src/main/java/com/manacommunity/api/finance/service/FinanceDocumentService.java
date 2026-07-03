package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.dto.FinanceDocumentDto;
import com.manacommunity.api.finance.entity.FinanceDocument;
import com.manacommunity.api.finance.entity.FinanceDocumentLine;
import com.manacommunity.api.finance.repository.FinanceDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceDocumentService {

    private final FinanceDocumentRepository repo;

    public List<FinanceDocumentDto> getAll(FinanceDocument.Type type) {
        List<FinanceDocument> docs = (type != null)
                ? repo.findByTypeOrderByDocDateDesc(type)
                : repo.findAllByOrderByDocDateDesc();
        return docs.stream().map(this::toDto).toList();
    }

    @Transactional
    public FinanceDocumentDto create(FinanceDocumentDto dto) {
        FinanceDocument doc = FinanceDocument.builder()
                .type(dto.getType())
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
                .code(dto.getCode())
                .lines(new ArrayList<>())
                .build();
        applyLines(doc, dto);
        FinanceDocument saved = repo.save(doc);
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode(prefix(saved.getType()) + "/" + String.format("%03d", saved.getId()));
            saved = repo.save(saved);
        }
        return toDto(saved);
    }

    @Transactional
    public FinanceDocumentDto update(Long id, FinanceDocumentDto dto) {
        FinanceDocument doc = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinanceDocument", id));
        doc.setType(dto.getType());
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
        if (!repo.existsById(id)) throw new ResourceNotFoundException("FinanceDocument", id);
        repo.deleteById(id);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────
    private void applyLines(FinanceDocument doc, FinanceDocumentDto dto) {
        if (dto.getItems() == null) return;
        for (FinanceDocumentDto.Line l : dto.getItems()) {
            boolean empty = (l.getItem() == null || l.getItem().isBlank())
                    && (l.getQty() == null || l.getQty() == 0);
            if (empty) continue;
            doc.getLines().add(FinanceDocumentLine.builder()
                    .document(doc)
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

    private String prefix(FinanceDocument.Type type) {
        return switch (type) {
            case INVOICE -> "INV";
            case ESTIMATE -> "EST";
            case SALES_ORDER -> "SLSODR";
            case CREDIT_NOTE -> "CN";
            case REFUND -> "REF";
        };
    }

    private FinanceDocumentDto toDto(FinanceDocument d) {
        return FinanceDocumentDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .type(d.getType())
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
                .items(d.getLines().stream().map(l -> FinanceDocumentDto.Line.builder()
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
