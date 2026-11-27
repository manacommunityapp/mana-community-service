package com.manacommunity.api.partnerapi.repository;

import com.manacommunity.api.partnerapi.entity.PartnerWebhookKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerWebhookKeyRepository extends JpaRepository<PartnerWebhookKey, Long> {
    Optional<PartnerWebhookKey> findByKeyId(String keyId);
    List<PartnerWebhookKey> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
    Optional<PartnerWebhookKey> findFirstByPartnerIdAndStatusOrderByCreatedAtDesc(String partnerId, String status);
}
