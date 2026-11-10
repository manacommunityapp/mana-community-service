package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.GovernanceResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceResolutionRepository extends JpaRepository<GovernanceResolution, Long> {
    List<GovernanceResolution> findByCommunityIdOrderByPassedOnDesc(Long communityId);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
