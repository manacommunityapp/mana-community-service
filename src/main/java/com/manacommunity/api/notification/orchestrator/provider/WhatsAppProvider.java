package com.manacommunity.api.notification.orchestrator.provider;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.WhatsAppTemplateRequest;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.WhatsAppTemplateResponse;

public interface WhatsAppProvider {
    WhatsAppTemplateResponse sendTemplateMessage(WhatsAppTemplateRequest request);
}
