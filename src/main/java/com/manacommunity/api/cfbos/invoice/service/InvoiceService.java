package com.manacommunity.api.cfbos.invoice.service;

import com.manacommunity.api.cfbos.invoice.dto.*;
import com.manacommunity.api.cfbos.invoice.engine.InvoiceEngine;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceEngine invoiceEngine;
    private final CfbosInvoiceRepository invoiceRepository;

    @Transactional
    public List<InvoiceResponse> generateInvoicesFromRun(Long billingRunId) {
        List<CfbosInvoice> invoices = invoiceEngine.generateInvoicesFromBillingRun(billingRunId);
        return invoices.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        CfbosInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Invoice", id));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByResident(Long residentId) {
        return invoiceRepository.findByResidentId(residentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public InvoiceResponse toResponse(CfbosInvoice inv) {
        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .propertyId(inv.getPropertyId())
                .residentId(inv.getResidentId())
                .billingRunId(inv.getBillingRunId())
                .invoiceType(inv.getInvoiceType())
                .status(inv.getStatus())
                .subtotal(inv.getSubtotal())
                .discountAmount(inv.getDiscountAmount())
                .taxableAmount(inv.getTaxableAmount())
                .cgstAmount(inv.getCgstAmount())
                .sgstAmount(inv.getSgstAmount())
                .igstAmount(inv.getIgstAmount())
                .totalTax(inv.getTotalTax())
                .totalAmount(inv.getTotalAmount())
                .amountPaid(inv.getAmountPaid())
                .balanceDue(inv.getBalanceDue())
                .billingPeriodStart(inv.getBillingPeriodStart())
                .billingPeriodEnd(inv.getBillingPeriodEnd())
                .notes(inv.getNotes())
                .terms(inv.getTerms())
                .isGstInvoice(inv.getIsGstInvoice())
                .communityGstin(inv.getCommunityGstin())
                .journalEntryId(inv.getJournalEntryId())
                .lines(inv.getLines() != null ? inv.getLines().stream().map(l ->
                        InvoiceResponse.InvoiceLineDto.builder()
                                .id(l.getId())
                                .chargeTypeId(l.getChargeTypeId())
                                .description(l.getDescription())
                                .hsnSacCode(l.getHsnSacCode())
                                .quantity(l.getQuantity())
                                .rate(l.getRate())
                                .amount(l.getAmount())
                                .discountAmount(l.getDiscountAmount())
                                .taxableAmount(l.getTaxableAmount())
                                .cgstRate(l.getCgstRate())
                                .cgstAmount(l.getCgstAmount())
                                .sgstRate(l.getSgstRate())
                                .sgstAmount(l.getSgstAmount())
                                .igstRate(l.getIgstRate())
                                .igstAmount(l.getIgstAmount())
                                .totalAmount(l.getTotalAmount())
                                .build()
                ).collect(Collectors.toList()) : List.of())
                .build();
    }
}
