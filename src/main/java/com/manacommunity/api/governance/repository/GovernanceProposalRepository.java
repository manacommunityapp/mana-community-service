package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.GovernanceProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceProposalRepository extends JpaRepository<GovernanceProposal, Long> {
    List<GovernanceProposal> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GovernanceProposal> findByCommunityIdAndCategoryOrderByCreatedAtDesc(Long communityId, String category);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
