package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.PaymentRequest;
import com.manacommunity.api.vendor.dto.PaymentResponse;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmsPaymentService {

    private final VmsPaymentRepository paymentRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsInvoiceRepository invoiceRepo;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getCommunityPayments(Long communityId, Pageable pageable) {
        return paymentRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getVendorPayments(Long vendorId, Pageable pageable) {
        return paymentRepo.findByVendorId(vendorId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id, Long communityId) {
        return toResponse(paymentRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id)));
    }

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest req, AppUser processedBy, Community community) {
        VmsVendor vendor = vendorRepo.findById(req.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));

        String paymentNumber = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        VmsPayment payment = VmsPayment.builder()
                .paymentNumber(paymentNumber)
                .vendor(vendor)
                .type(req.getType() != null ? VmsPayment.PaymentType.valueOf(req.getType()) : VmsPayment.PaymentType.FULL)
                .amount(req.getAmount())
                .netAmount(req.getAmount())
                .paymentMethod(req.getPaymentMethod())
                .transactionId(req.getTransactionId())
                .paymentDate(req.getPaymentDate())
                .notes(req.getNotes())
                .processedBy(processedBy)
                .community(community)
                .build();

        if (req.getInvoiceId() != null) {
            VmsInvoice invoice = invoiceRepo.findById(req.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", req.getInvoiceId()));
            payment.setInvoice(invoice);
        }

        VmsPayment saved = paymentRepo.save(payment);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse updateStatus(Long id, String status, Long communityId) {
        VmsPayment payment = paymentRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        payment.setStatus(VmsPayment.PaymentProcessStatus.valueOf(status));
        return toResponse(paymentRepo.save(payment));
    }

    private PaymentResponse toResponse(VmsPayment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .vendor(PaymentResponse.VendorRef.builder()
                        .id(p.getVendor().getId())
                        .businessName(p.getVendor().getBusinessName())
                        .build())
                .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                .bookingId(p.getBooking() != null ? p.getBooking().getId() : null)
                .type(p.getType() != null ? p.getType().name() : null)
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .amount(p.getAmount())
                .gstAmount(p.getGstAmount())
                .tdsAmount(p.getTdsAmount())
                .commissionAmount(p.getCommissionAmount())
                .netAmount(p.getNetAmount())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .paymentDate(p.getPaymentDate())
                .notes(p.getNotes())
                .communityId(p.getCommunity() != null ? p.getCommunity().getId() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
