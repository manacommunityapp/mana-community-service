package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityBallot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityBallotRepository extends JpaRepository<CommunityBallot, Long> {
    List<CommunityBallot> findByCommunityIdOrderByStartDateDesc(Long communityId);
    List<CommunityBallot> findByCommunityIdAndStatusOrderByStartDateDesc(Long communityId, String status);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
