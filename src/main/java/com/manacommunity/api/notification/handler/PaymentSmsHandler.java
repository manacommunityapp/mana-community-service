package com.manacommunity.api.notification.handler;

import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.event.DonationSuccessEvent;
import com.manacommunity.api.notification.event.PaymentCapturedEvent;
import com.manacommunity.api.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSmsHandler {

    private final SmsService smsService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        try {
            smsService.send(SendSmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .templateCode("PAYMENT_SUCCESSFUL")
                    .messageType(MessageType.PAYMENT)
                    .userId(event.getUserId())
                    .variables(Map.of(
                            "name", event.getFullName(),
                            "amount", event.getAmount().toPlainString(),
                            "paymentId", event.getPaymentId(),
                            "orderId", event.getOrderId()))
                    .referenceType("PAYMENT")
                    .referenceId(null)
                    .build());
        } catch (Exception ex) {
            log.warn("PaymentCaptured SMS failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDonationSuccess(DonationSuccessEvent event) {
        try {
            smsService.send(SendSmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .templateCode("DONATION_RECEIVED")
                    .messageType(MessageType.PAYMENT)
                    .userId(event.getUserId())
                    .variables(Map.of(
                            "name", event.getFullName(),
                            "amount", event.getAmount().toPlainString(),
                            "receiptId", event.getReceiptId()))
                    .build());
        } catch (Exception ex) {
            log.warn("Donation SMS failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }
}
