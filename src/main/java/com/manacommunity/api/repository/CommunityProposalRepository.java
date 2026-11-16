package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityProposalRepository extends JpaRepository<CommunityProposal, Long> {
    List<CommunityProposal> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<CommunityProposal> findByCommunityIdAndCategoryOrderByCreatedAtDesc(Long communityId, String category);
    long countByCommunityIdAndStatusIn(Long communityId, List<String> statuses);
}
