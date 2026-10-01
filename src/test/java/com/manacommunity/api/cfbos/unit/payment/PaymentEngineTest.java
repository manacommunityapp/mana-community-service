package com.manacommunity.api.cfbos.unit.payment;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.payment.dto.RecordPaymentRequest;
import com.manacommunity.api.cfbos.payment.engine.PaymentEngine;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.cfbos.payment.repository.CfbosReceiptRepository;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEngineTest {

    @Mock private CfbosPaymentRepository paymentRepository;
    @Mock private CfbosReceiptRepository receiptRepository;
    @Mock private CfbosInvoiceRepository invoiceRepository;
    @Mock private DocumentSequenceService documentSequenceService;
    @Mock private AccountingEngine accountingEngine;

    private PaymentEngine paymentEngine;

    @BeforeEach
    void setUp() {
        paymentEngine = new PaymentEngine(
                paymentRepository, receiptRepository, invoiceRepository, documentSequenceService, accountingEngine
        );
    }

    @Test
    @DisplayName("Process payment settles invoice and issues receipt")
    void processPaymentSettlesInvoice() {
        CfbosInvoice invoice = CfbosInvoice.builder()
                .id(1L)
                .residentId(10L)
                .dueDate(LocalDate.of(2026, 4, 15))
                .status(InvoiceStatus.ISSUED)
                .totalAmount(new BigDecimal("5000.00"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("5000.00"))
                .build();

        when(invoiceRepository.findByResidentId(10L)).thenReturn(List.of(invoice));
        when(documentSequenceService.nextNumber(any(), anyString())).thenReturn("RCP-2026-0001");
        when(paymentRepository.save(any(CfbosPayment.class))).thenAnswer(i -> {
            CfbosPayment p = i.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(receiptRepository.save(any(CfbosReceipt.class))).thenAnswer(i -> {
            CfbosReceipt r = i.getArgument(0);
            r.setId(200L);
            return r;
        });

        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .residentId(10L)
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethodType.UPI)
                .paymentDate(LocalDate.of(2026, 4, 10))
                .gatewayReference("UPI-TXN-123456")
                .build();

        CfbosPayment payment = paymentEngine.processPayment(req);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getAppliedAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(payment.getUnappliedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
