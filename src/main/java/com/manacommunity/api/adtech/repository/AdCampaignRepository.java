package com.manacommunity.api.adtech.repository;

import com.manacommunity.api.adtech.entity.AdCampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdCampaignRepository extends JpaRepository<AdCampaignEntity, Long> {

    Optional<AdCampaignEntity> findByCampaignIdAndDeletedFalse(UUID campaignId);

    List<AdCampaignEntity> findByDeletedFalseOrderByCreatedAtDesc();

    List<AdCampaignEntity> findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndDeletedFalse(
            String status, Instant now1, Instant now2);

    @Modifying
    @Query("""
        UPDATE AdCampaignEntity c
           SET c.spentPaise = c.spentPaise + :costPaise,
               c.status = CASE
                   WHEN (c.spentPaise + :costPaise) >= c.totalBudgetPaise THEN 'EXHAUSTED'
                   ELSE c.status
               END,
               c.updatedAt = CURRENT_TIMESTAMP
         WHERE c.campaignId = :campaignId
           AND c.deleted = false
        """)
    int recordSpendAndCheckExhausted(@Param("campaignId") UUID campaignId, @Param("costPaise") long costPaise);
}
