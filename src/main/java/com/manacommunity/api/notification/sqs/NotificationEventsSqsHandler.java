package com.manacommunity.api.notification.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.WhatsAppTemplateRequest;
import com.manacommunity.api.notification.orchestrator.provider.WhatsAppProvider;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsMessagePublisher;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SQS consumer/bridge for {@code mana-notification-events}.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>Legacy SMS notification fan-outs ({@code REGISTRATION_CONFIRMATION}, {@code PAYMENT_SUCCESS}, {@code EVENT_CANCELLED})</li>
 *   <li>{@code UNIFIED_PUSH_DISPATCH} — Expo push notifications routed from {@code UnifiedNotificationServiceImpl}</li>
 *   <li>{@code UNIFIED_WHATSAPP_DISPATCH} — WhatsApp template messages routed from {@code UnifiedNotificationServiceImpl}</li>
 * </ul>
 */
@Slf4j
@Component
public class NotificationEventsSqsHandler implements SqsMessageHandler {

    private final SmsService smsService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final ExpoPushService expoPushService;
    private final WhatsAppProvider whatsAppProvider;

    @Autowired(required = false)
    private SqsMessagePublisher sqsMessagePublisher;

    public NotificationEventsSqsHandler(
            SmsService smsService,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper) {
        this(smsService, sqsProperties, objectMapper, null, null);
    }

    @Autowired
    public NotificationEventsSqsHandler(
            SmsService smsService,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper,
            @Autowired(required = false) ExpoPushService expoPushService,
            @Autowired(required = false) WhatsAppProvider whatsAppProvider) {
        this.smsService = smsService;
        this.sqsProperties = sqsProperties;
        this.objectMapper = objectMapper;
        this.expoPushService = expoPushService;
        this.whatsAppProvider = whatsAppProvider;
    }

    public record UnifiedPushPayload(
            Long userId,
            String title,
            String body,
            Map<String, Object> data,
            String channelId
    ) {}

    public record UnifiedWhatsAppPayload(
            String recipientPhone,
            String templateName,
            Map<String, String> parameters
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getNotificationEvents();
    }

    /**
     * Dispatches a {@link SendSmsRequest} through SQS (with direct fallback when SQS is off).
     */
    public void dispatch(String eventType, SendSmsRequest request) {
        if (sqsMessagePublisher != null) {
            sqsMessagePublisher.publish(queueName(), eventType, request, () -> smsService.send(request));
        } else {
            smsService.send(request);
        }
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        switch (envelope.eventType()) {
            case "UNIFIED_PUSH_DISPATCH" -> deliverUnifiedPush(
                    objectMapper.readValue(envelope.payloadJson(), UnifiedPushPayload.class));
            case "UNIFIED_WHATSAPP_DISPATCH" -> deliverUnifiedWhatsApp(
                    objectMapper.readValue(envelope.payloadJson(), UnifiedWhatsAppPayload.class));
            default -> {
                SendSmsRequest request = objectMapper.readValue(envelope.payloadJson(), SendSmsRequest.class);
                smsService.send(request);
            }
        }
    }

    public void deliverUnifiedPush(UnifiedPushPayload payload) {
        if (payload == null || payload.userId() == null) return;
        if (expoPushService == null) {
            log.warn("ExpoPushService unavailable for unified push userId={}", payload.userId());
            return;
        }
        String channelId = payload.channelId() != null ? payload.channelId() : "default";
        expoPushService.sendToUser(
                payload.userId(),
                payload.title(),
                payload.body(),
                payload.data(),
                channelId
        );
    }

    public void deliverUnifiedWhatsApp(UnifiedWhatsAppPayload payload) {
        if (payload == null || payload.recipientPhone() == null || payload.recipientPhone().isBlank()) return;
        if (whatsAppProvider == null) {
            log.warn("WhatsAppProvider unavailable for phone={}", payload.recipientPhone());
            return;
        }
        whatsAppProvider.sendTemplateMessage(WhatsAppTemplateRequest.builder()
                .recipientPhone(payload.recipientPhone())
                .templateName(payload.templateName() != null ? payload.templateName() : "community_alert")
                .parameters(payload.parameters() != null ? payload.parameters() : Map.of())
                .build());
    }
}
