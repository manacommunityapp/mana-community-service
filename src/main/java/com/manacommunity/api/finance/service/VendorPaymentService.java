package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.entity.VendorPayment;
import com.manacommunity.api.finance.repository.VendorPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorPaymentService {

    private final VendorPaymentRepository repo;

    public List<VendorPayment> getAll(VendorPayment.PaymentType type) {
        return (type != null)
                ? repo.findByPaymentTypeOrderByPaymentDateDesc(type)
                : repo.findAllByOrderByPaymentDateDesc();
    }

    @Transactional
    public VendorPayment create(VendorPayment in) {
        in.setId(null);
        if (in.getPaymentType() == null) in.setPaymentType(VendorPayment.PaymentType.PAID_BILL);
        if (in.getPaymentDate() == null) in.setPaymentDate(LocalDate.now());
        if (in.getStatus() == null || in.getStatus().isBlank()) in.setStatus("Paid");
        VendorPayment saved = repo.save(in);
        if (saved.getCode() == null || saved.getCode().isBlank()) {
            saved.setCode("VPAY/" + String.format("%03d", saved.getId()));
            saved = repo.save(saved);
        }
        return saved;
    }

    @Transactional
    public VendorPayment update(Long id, VendorPayment in) {
        VendorPayment p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VendorPayment", id));
        if (in.getPaymentType() != null) p.setPaymentType(in.getPaymentType());
        p.setVendorId(in.getVendorId());
        p.setVendorName(in.getVendorName());
        if (in.getPaymentDate() != null) p.setPaymentDate(in.getPaymentDate());
        p.setAmount(in.getAmount());
        p.setPaymentMode(in.getPaymentMode());
        p.setPaidFrom(in.getPaidFrom());
        p.setReference(in.getReference());
        if (in.getStatus() != null && !in.getStatus().isBlank()) p.setStatus(in.getStatus());
        p.setNotes(in.getNotes());
        if (in.getCode() != null && !in.getCode().isBlank()) p.setCode(in.getCode());
        return repo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("VendorPayment", id);
        repo.deleteById(id);
    }
}
