package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import com.manacommunity.api.notification.orchestrator.provider.WhatsAppProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/orchestrate")
@RequiredArgsConstructor
public class UnifiedNotificationController {

    private final UnifiedNotificationService notificationService;
    private final WhatsAppProvider whatsAppProvider;

    @PostMapping("/send")
    public ResponseEntity<UnifiedNotificationResult> sendNotification(
            @Valid @RequestBody UnifiedNotificationRequest request) {
        UnifiedNotificationResult result = notificationService.orchestrate(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit-logs/{userId}")
    public ResponseEntity<Page<NotificationAuditLog>> getAuditLogs(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<NotificationAuditLog> logs = notificationService.getAuditLogsForUser(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/whatsapp/template")
    public ResponseEntity<WhatsAppTemplateResponse> sendWhatsAppTemplate(
            @Valid @RequestBody WhatsAppTemplateRequest request) {
        WhatsAppTemplateResponse response = whatsAppProvider.sendTemplateMessage(request);
        return ResponseEntity.ok(response);
    }
}
