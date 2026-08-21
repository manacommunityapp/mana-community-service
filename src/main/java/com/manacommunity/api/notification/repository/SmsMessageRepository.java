package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.SmsMessage;
import com.manacommunity.api.notification.enums.SmsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {

    Optional<SmsMessage> findByIdempotencyKey(String idempotencyKey);

    Optional<SmsMessage> findByProviderMessageId(String providerMessageId);

    Page<SmsMessage> findByStatus(SmsStatus status, Pageable pageable);

    Page<SmsMessage> findByUserId(Long userId, Pageable pageable);

    List<SmsMessage> findByStatusAndNextRetryAtBefore(SmsStatus status, LocalDateTime cutoff);

    /** Picks up FAILED messages eligible for retry (retryCount < maxRetries). */
    @Query("""
            SELECT m FROM SmsMessage m
            WHERE m.status = com.manacommunity.api.notification.enums.SmsStatus.FAILED
              AND m.retryCount < m.maxRetries
              AND m.nextRetryAt <= :now
            ORDER BY m.priority ASC, m.nextRetryAt ASC
            """)
    List<SmsMessage> findMessagesForRetry(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(m) FROM SmsMessage m WHERE m.status = :status")
    long countByStatus(@Param("status") SmsStatus status);

    @Query("""
            SELECT m FROM SmsMessage m
            WHERE m.campaignId = :campaignId
            ORDER BY m.createdAt DESC
            """)
    Page<SmsMessage> findByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);
}
