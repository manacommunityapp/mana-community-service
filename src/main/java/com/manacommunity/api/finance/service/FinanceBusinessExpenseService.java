package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.dto.FinanceBusinessExpenseDto;
import com.manacommunity.api.finance.entity.FinanceBusinessExpense;
import com.manacommunity.api.finance.entity.FinanceLineItem;
import com.manacommunity.api.finance.repository.FinanceBusinessExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Business logic for business expenses / bills (Expense → Business Expenses menu). */
@Service
@RequiredArgsConstructor
public class FinanceBusinessExpenseService {

    private static final String CODE_PREFIX = "EXP";

    private final FinanceBusinessExpenseRepository repo;

    @Transactional(readOnly = true)
    public List<FinanceBusinessExpenseDto> getAll(String status) {
        List<FinanceBusinessExpense> docs = (status != null && !status.isBlank())
                ? repo.findByStatusOrderByDocDateDesc(status)
                : repo.findAllByOrderByDocDateDesc();
        return docs.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FinanceBusinessExpenseDto get(Long id) {
        return toDto(require(id));
    }

    @Transactional
    public FinanceBusinessExpenseDto create(FinanceBusinessExpenseDto dto) {
        FinanceBusinessExpense doc = FinanceBusinessExpense.builder()
                .code(dto.getCode())
                .status(dto.getStatus())
                .vendorId(dto.getVendorId())
                .vendorName(dto.getVendorName())
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

        FinanceBusinessExpense saved = repo.save(doc);
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode(CODE_PREFIX + "/" + String.format("%03d", saved.getId()));
            saved = repo.save(saved);
        }
        return toDto(saved);
    }

    @Transactional
    public FinanceBusinessExpenseDto update(Long id, FinanceBusinessExpenseDto dto) {
        FinanceBusinessExpense doc = require(id);
        doc.setStatus(dto.getStatus());
        doc.setVendorId(dto.getVendorId());
        doc.setVendorName(dto.getVendorName());
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
        if (!repo.existsById(id)) throw new ResourceNotFoundException("FinanceBusinessExpense", id);
        repo.deleteById(id);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private FinanceBusinessExpense require(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("FinanceBusinessExpense", id));
    }

    private void applyLines(FinanceBusinessExpense doc, FinanceBusinessExpenseDto dto) {
        if (dto.getItems() == null) return;
        for (FinanceBusinessExpenseDto.Line l : dto.getItems()) {
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

    private FinanceBusinessExpenseDto toDto(FinanceBusinessExpense d) {
        return FinanceBusinessExpenseDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .status(d.getStatus())
                .vendorId(d.getVendorId())
                .vendorName(d.getVendorName())
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
                .items(d.getLines().stream().map(l -> FinanceBusinessExpenseDto.Line.builder()
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
