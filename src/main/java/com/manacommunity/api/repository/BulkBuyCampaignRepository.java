package com.manacommunity.api.repository;

import com.manacommunity.api.model.BulkBuyCampaign;
import com.manacommunity.api.model.BulkBuyCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkBuyCampaignRepository extends JpaRepository<BulkBuyCampaign, Long> {

    List<BulkBuyCampaign> findByCommunityIdOrderByDeadlineDateAsc(Long communityId);

    List<BulkBuyCampaign> findByCommunityIdAndStatusOrderByDeadlineDateAsc(Long communityId, BulkBuyCampaignStatus status);

    Optional<BulkBuyCampaign> findByIdAndCommunityId(Long id, Long communityId);
}
