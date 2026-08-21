package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.BulkSmsCampaign;
import com.manacommunity.api.notification.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BulkSmsCampaignRepository extends JpaRepository<BulkSmsCampaign, Long> {

    Page<BulkSmsCampaign> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<BulkSmsCampaign> findByStatus(CampaignStatus status, Pageable pageable);

    /** Campaigns approved and scheduled to start before 'now' that are still APPROVED. */
    @Query("""
            SELECT c FROM BulkSmsCampaign c
            WHERE c.status = com.manacommunity.api.notification.enums.CampaignStatus.APPROVED
              AND c.scheduledAt <= :now
            ORDER BY c.scheduledAt ASC
            """)
    List<BulkSmsCampaign> findDueCampaigns(@Param("now") LocalDateTime now);
}
