package com.manacommunity.api.notification.orchestrator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppDeliveryLogRepository extends JpaRepository<WhatsAppDeliveryLog, Long> {
    List<WhatsAppDeliveryLog> findByRecipientPhoneOrderByCreatedAtDesc(String recipientPhone);
}
