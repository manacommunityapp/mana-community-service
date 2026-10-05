package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import com.manacommunity.api.notification.orchestrator.engine.NotificationRule;
import com.manacommunity.api.notification.orchestrator.event.DomainEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UnifiedNotificationService {

    UnifiedNotificationResult orchestrate(UnifiedNotificationRequest request);

    List<UnifiedNotificationResult> handleDomainEvent(DomainEvent event);

    UnifiedNotificationResult retryDelivery(Long auditLogId);

    List<NotificationRule> getNotificationRules();

    Page<NotificationAuditLog> getAuditLogsForUser(Long userId, Pageable pageable);
}
