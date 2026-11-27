package com.manacommunity.api.partnerapi.repository;

import com.manacommunity.api.partnerapi.entity.WebhookNonceJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookNonceJournalRepository extends JpaRepository<WebhookNonceJournal, Long> {
    Optional<WebhookNonceJournal> findByPartnerIdAndNonce(String partnerId, String nonce);
}
