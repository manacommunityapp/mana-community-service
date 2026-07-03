package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.entity.FinanceReceipt;
import com.manacommunity.api.finance.repository.FinanceReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceReceiptService {

    private final FinanceReceiptRepository repo;

    public List<FinanceReceipt> getAll(FinanceReceipt.ReceiptType type) {
        return (type != null)
                ? repo.findByReceiptTypeOrderByReceiptDateDesc(type)
                : repo.findAllByOrderByReceiptDateDesc();
    }

    @Transactional
    public FinanceReceipt create(FinanceReceipt in) {
        in.setId(null);
        if (in.getReceiptDate() == null) in.setReceiptDate(LocalDate.now());
        FinanceReceipt saved = repo.save(in);
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode(prefix(saved.getReceiptType()) + "/" + String.format("%03d", saved.getId()));
            saved = repo.save(saved);
        }
        return saved;
    }

    @Transactional
    public FinanceReceipt update(Long id, FinanceReceipt in) {
        FinanceReceipt r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinanceReceipt", id));
        r.setReceiptType(in.getReceiptType());
        r.setCustomerId(in.getCustomerId());
        r.setCustomerName(in.getCustomerName());
        if (in.getReceiptDate() != null) r.setReceiptDate(in.getReceiptDate());
        r.setAmount(in.getAmount());
        r.setPaymentMode(in.getPaymentMode());
        r.setReference(in.getReference());
        r.setNotes(in.getNotes());
        if (in.getCode() != null && !in.getCode().isBlank()) r.setCode(in.getCode());
        return repo.save(r);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("FinanceReceipt", id);
        repo.deleteById(id);
    }

    private String prefix(FinanceReceipt.ReceiptType type) {
        return switch (type) {
            case INVOICE -> "RCPT";
            case ADVANCE -> "ARCPT";
            case OTHER -> "OINC";
        };
    }
}
