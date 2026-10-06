package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.model.TransactionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionPaymentPillar {

    private final WalletEngine walletEngine;
    private final CfbosPaymentRepository paymentRepository;
    private final DocumentSequenceService documentSequenceService;

    @Transactional
    public CfbosPayment capturePayment(TransactionIntent intent, String transactionNumber) {
        if (intent.getAmount() == null || intent.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CfbosException("Transaction amount must be strictly positive");
        }

        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String paymentNumber;
        try {
            paymentNumber = documentSequenceService.nextNumber(DocumentType.RECEIPT, year);
        } catch (Exception e) {
            paymentNumber = "PAY-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        PaymentMethodType methodType = mapPaymentMethod(intent.getPaymentMethod());

        // 1. If paying via resident wallet, debit balance
        if (intent.getPaymentMethod() == TransactionPaymentMethod.WALLET) {
            walletEngine.debitWallet(
                    intent.getPayerId(),
                    intent.getAmount(),
                    WalletTransactionType.BILL_PAYMENT,
                    intent.getDomain().name(),
                    null,
                    "Payment for " + intent.getDomain() + " [" + transactionNumber + "]"
            );
        }

        // 2. Persist Payment Record
        CfbosPayment payment = CfbosPayment.builder()
                .paymentNumber(paymentNumber)
                .paymentDate(now)
                .residentId(intent.getPayerId())
                .propertyId(intent.getPropertyId() != null ? intent.getPropertyId() : 0L)
                .paymentMethod(methodType)
                .paymentMode(intent.getPaymentMethod() == TransactionPaymentMethod.CASH ? "OFFLINE" : "ONLINE")
                .amount(intent.getAmount())
                .appliedAmount(intent.getAmount())
                .unappliedAmount(BigDecimal.ZERO)
                .gatewayReference(intent.getGatewayReference())
                .remarks(intent.getNarration())
                .status(PaymentStatus.SUCCESS)
                .build();

        return paymentRepository.save(payment);
    }

    private PaymentMethodType mapPaymentMethod(TransactionPaymentMethod method) {
        if (method == null) return PaymentMethodType.UPI;
        return switch (method) {
            case WALLET -> PaymentMethodType.WALLET;
            case UPI -> PaymentMethodType.UPI;
            case CARD -> PaymentMethodType.CREDIT_CARD;
            case NET_BANKING -> PaymentMethodType.NET_BANKING;
            case GATEWAY_RAZORPAY -> PaymentMethodType.UPI;
            case CASH -> PaymentMethodType.CASH;
        };
    }
}
