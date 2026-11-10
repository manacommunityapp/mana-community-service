package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityResolutionRepository extends JpaRepository<CommunityResolution, Long> {
    List<CommunityResolution> findByCommunityIdOrderByPassedDateDesc(Long communityId);
    List<CommunityResolution> findByCommunityIdAndTitleContainingIgnoreCaseOrderByPassedDateDesc(Long communityId, String search);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
