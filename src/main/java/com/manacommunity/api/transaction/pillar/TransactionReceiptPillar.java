package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.repository.CfbosReceiptRepository;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.transaction.model.TransactionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionReceiptPillar {

    private final CfbosReceiptRepository receiptRepository;
    private final DocumentSequenceService documentSequenceService;

    @Transactional
    public CfbosReceipt issueReceipt(TransactionIntent intent, CfbosPayment payment, String transactionNumber) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());

        String receiptNumber;
        try {
            String seq = documentSequenceService.nextNumber(DocumentType.RECEIPT, year);
            receiptNumber = "RCP-" + seq.replace("REC-", "").replace("DOC-", "");
        } catch (Exception e) {
            receiptNumber = "RCP-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        PaymentMethodType method = payment != null ? payment.getPaymentMethod() : PaymentMethodType.UPI;

        CfbosReceipt receipt = CfbosReceipt.builder()
                .receiptNumber(receiptNumber)
                .receiptDate(now)
                .paymentId(payment != null ? payment.getId() : 0L)
                .residentId(intent.getPayerId())
                .amount(intent.getAmount())
                .paymentMethod(method)
                .narration("Receipt for " + intent.getDomain() + " [" + transactionNumber + "]")
                .build();

        return receiptRepository.save(receipt);
    }
}
