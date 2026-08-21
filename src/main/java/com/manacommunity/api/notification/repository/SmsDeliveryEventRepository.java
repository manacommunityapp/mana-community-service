package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.SmsDeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsDeliveryEventRepository extends JpaRepository<SmsDeliveryEvent, Long> {

    boolean existsByProviderMessageIdAndEventType(String providerMessageId, String eventType);

    Optional<SmsDeliveryEvent> findByProviderMessageIdAndEventType(String providerMessageId, String eventType);

    List<SmsDeliveryEvent> findBySmsMessageIdOrderByReceivedAtDesc(Long smsMessageId);
}
