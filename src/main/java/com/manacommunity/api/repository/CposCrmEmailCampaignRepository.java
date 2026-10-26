package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmEmailCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CposCrmEmailCampaignRepository extends JpaRepository<CposCrmEmailCampaign, Long> {
    List<CposCrmEmailCampaign> findByCommunityIdOrderBySentAtDesc(Long communityId);
}
