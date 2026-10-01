package com.manacommunity.api.cfbos.payment.service;

import com.manacommunity.api.cfbos.payment.dto.PaymentResponse;
import com.manacommunity.api.cfbos.payment.dto.RecordPaymentRequest;
import com.manacommunity.api.cfbos.payment.engine.PaymentEngine;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.cfbos.payment.repository.CfbosReceiptRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentEngine paymentEngine;
    private final CfbosPaymentRepository paymentRepository;
    private final CfbosReceiptRepository receiptRepository;

    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        CfbosPayment payment = paymentEngine.processPayment(request);
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        CfbosPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Payment", id));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForResident(Long residentId) {
        return paymentRepository.findByResidentId(residentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CfbosReceipt> getReceiptsForResident(Long residentId) {
        return receiptRepository.findByResidentId(residentId);
    }

    private PaymentResponse toResponse(CfbosPayment p) {
        CfbosReceipt r = p.getReceiptId() != null ? receiptRepository.findById(p.getReceiptId()).orElse(null) : null;
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .paymentDate(p.getPaymentDate())
                .residentId(p.getResidentId())
                .propertyId(p.getPropertyId())
                .paymentMethod(p.getPaymentMethod())
                .paymentMode(p.getPaymentMode())
                .amount(p.getAmount())
                .appliedAmount(p.getAppliedAmount())
                .unappliedAmount(p.getUnappliedAmount())
                .gatewayReference(p.getGatewayReference())
                .status(p.getStatus())
                .receiptNumber(r != null ? r.getReceiptNumber() : null)
                .journalEntryId(p.getJournalEntryId())
                .remarks(p.getRemarks())
                .lines(p.getLines() != null ? p.getLines().stream().map(l ->
                        PaymentResponse.PaymentLineDto.builder()
                                .id(l.getId())
                                .invoiceId(l.getInvoiceId())
                                .allocatedAmount(l.getAllocatedAmount())
                                .allocationDate(l.getAllocationDate())
                                .build()
                ).collect(Collectors.toList()) : List.of())
                .build();
    }
}
