package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UnifiedNotificationService {
    UnifiedNotificationResult orchestrate(UnifiedNotificationRequest request);
    Page<NotificationAuditLog> getAuditLogsForUser(Long userId, Pageable pageable);
}
