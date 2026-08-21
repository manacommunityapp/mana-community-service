package com.manacommunity.api.controller;

import com.manacommunity.api.notification.entity.SmsDeliveryEvent;
import com.manacommunity.api.notification.entity.SmsMessage;
import com.manacommunity.api.notification.enums.SmsStatus;
import com.manacommunity.api.notification.repository.SmsDeliveryEventRepository;
import com.manacommunity.api.notification.repository.SmsMessageRepository;
import com.manacommunity.api.notification.webhook.TwilioWebhookVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/sms/webhook")
@RequiredArgsConstructor
@Slf4j
public class SmsWebhookController {

    private final TwilioWebhookVerifier verifier;
    private final SmsDeliveryEventRepository deliveryEventRepo;
    private final SmsMessageRepository smsMessageRepo;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        String requestUrl = request.getRequestURL().toString();
        if (!verifier.verify(signature, requestUrl, params)) {
            log.warn("Invalid Twilio webhook signature from {}", request.getRemoteAddr());
            return ResponseEntity.status(403).build();
        }

        String messageSid = params.get("MessageSid");
        String messageStatus = params.get("MessageStatus");

        if (messageSid == null || messageStatus == null) {
            return ResponseEntity.badRequest().build();
        }

        // Idempotency check
        if (deliveryEventRepo.existsByProviderMessageIdAndEventType(messageSid, messageStatus)) {
            log.debug("Duplicate webhook for messageSid={} status={}", messageSid, messageStatus);
            return ResponseEntity.ok().build();
        }

        SmsDeliveryEvent event = SmsDeliveryEvent.builder()
                .providerMessageId(messageSid)
                .eventType(messageStatus)
                .rawPayload(params.toString())
                .eventTimestamp(LocalDateTime.now())
                .build();

        // Wire back to our SmsMessage
        smsMessageRepo.findByProviderMessageId(messageSid).ifPresent(msg -> {
            event.setSmsMessageId(msg.getId());
            updateMessageStatus(msg, messageStatus);
        });

        deliveryEventRepo.save(event);
        return ResponseEntity.ok().build();
    }

    private void updateMessageStatus(SmsMessage msg, String twilioStatus) {
        switch (twilioStatus.toLowerCase()) {
            case "delivered" -> {
                msg.setStatus(SmsStatus.DELIVERED);
                msg.setDeliveredAt(LocalDateTime.now());
            }
            case "failed", "undelivered" -> {
                if (msg.getStatus() != SmsStatus.DLQ) {
                    msg.setStatus(SmsStatus.FAILED);
                    msg.setFailedAt(LocalDateTime.now());
                }
            }
            default -> log.debug("Unhandled Twilio status '{}' for msg={}", twilioStatus, msg.getId());
        }
        smsMessageRepo.save(msg);
    }
}
