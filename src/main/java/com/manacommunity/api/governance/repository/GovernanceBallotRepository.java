package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.GovernanceBallot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceBallotRepository extends JpaRepository<GovernanceBallot, Long> {
    List<GovernanceBallot> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GovernanceBallot> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
