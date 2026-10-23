package com.manacommunity.api.notification.orchestrator.provider;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.WhatsAppTemplateRequest;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.WhatsAppTemplateResponse;
import com.manacommunity.api.notification.orchestrator.WhatsAppDeliveryLog;
import com.manacommunity.api.notification.orchestrator.WhatsAppDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockWhatsAppProvider implements WhatsAppProvider {

    private final WhatsAppDeliveryLogRepository waLogRepository;

    @Override
    public WhatsAppTemplateResponse sendTemplateMessage(WhatsAppTemplateRequest request) {
        log.info("[MOCK-WHATSAPP] Dispatching template '{}' to phone '{}'", 
                request.getTemplateName(), request.getRecipientPhone());

        String messageId = "wam_" + UUID.randomUUID().toString().substring(0, 12);

        WhatsAppDeliveryLog deliveryLog = WhatsAppDeliveryLog.builder()
                .recipientPhone(request.getRecipientPhone())
                .templateName(request.getTemplateName())
                .parametersJson(request.getParameters() != null ? request.getParameters().toString() : "{}")
                .status("SENT")
                .providerMessageId(messageId)
                .build();

        waLogRepository.save(deliveryLog);

        return WhatsAppTemplateResponse.builder()
                .success(true)
                .providerMessageId(messageId)
                .status("SENT")
                .build();
    }
}
