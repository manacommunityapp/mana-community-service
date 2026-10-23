package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.EmergencyNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyNotificationRepository extends JpaRepository<EmergencyNotification, Long> {
    List<EmergencyNotification> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
    List<EmergencyNotification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}
