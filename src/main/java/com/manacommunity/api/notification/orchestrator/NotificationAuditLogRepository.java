package com.manacommunity.api.notification.orchestrator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationAuditLogRepository extends JpaRepository<NotificationAuditLog, Long> {
    Page<NotificationAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<NotificationAuditLog> findByNotificationId(String notificationId);
}
