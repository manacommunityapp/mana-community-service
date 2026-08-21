package com.manacommunity.api.notification.handler;

import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.event.RegistrationApprovedEvent;
import com.manacommunity.api.notification.event.RegistrationConfirmedEvent;
import com.manacommunity.api.notification.event.RegistrationRejectedEvent;
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
public class RegistrationSmsHandler {

    private final SmsService smsService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationConfirmed(RegistrationConfirmedEvent event) {
        try {
            smsService.send(SendSmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .templateCode("REGISTRATION_CONFIRMED")
                    .messageType(MessageType.TRANSACTIONAL)
                    .userId(event.getUserId())
                    .variables(Map.of(
                            "name", event.getFullName(),
                            "community", event.getCommunityName()))
                    .build());
        } catch (Exception ex) {
            log.warn("RegistrationConfirmed SMS failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationApproved(RegistrationApprovedEvent event) {
        try {
            smsService.send(SendSmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .templateCode("REGISTRATION_APPROVED")
                    .messageType(MessageType.TRANSACTIONAL)
                    .userId(event.getUserId())
                    .variables(Map.of(
                            "name", event.getFullName(),
                            "community", event.getCommunityName()))
                    .build());
        } catch (Exception ex) {
            log.warn("RegistrationApproved SMS failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationRejected(RegistrationRejectedEvent event) {
        try {
            smsService.send(SendSmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .templateCode("REGISTRATION_REJECTED")
                    .messageType(MessageType.TRANSACTIONAL)
                    .userId(event.getUserId())
                    .variables(Map.of(
                            "name", event.getFullName(),
                            "reason", event.getReason() != null ? event.getReason() : ""))
                    .build());
        } catch (Exception ex) {
            log.warn("RegistrationRejected SMS failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }
}
