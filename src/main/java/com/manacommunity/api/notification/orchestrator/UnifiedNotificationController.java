package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import com.manacommunity.api.notification.orchestrator.engine.NotificationRule;
import com.manacommunity.api.notification.orchestrator.event.DomainEvent;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/orchestrator")
@RequiredArgsConstructor
public class UnifiedNotificationController {

    private final UnifiedNotificationService notificationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/dispatch")
    public ResponseEntity<UnifiedNotificationResult> dispatch(@Valid @RequestBody UnifiedNotificationRequest request) {
        return ResponseEntity.ok(notificationService.orchestrate(request));
    }

    @PostMapping("/events")
    public ResponseEntity<List<UnifiedNotificationResult>> handleDomainEvent(@Valid @RequestBody DomainEvent event) {
        return ResponseEntity.ok(notificationService.handleDomainEvent(event));
    }

    @PostMapping("/retry/{auditLogId}")
    public ResponseEntity<UnifiedNotificationResult> retryDelivery(@PathVariable Long auditLogId) {
        return ResponseEntity.ok(notificationService.retryDelivery(auditLogId));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<NotificationRule>> getRules() {
        return ResponseEntity.ok(notificationService.getNotificationRules());
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<NotificationAuditLog>> getMyAuditLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(notificationService.getAuditLogsForUser(user.getId(), pageable));
    }
}
