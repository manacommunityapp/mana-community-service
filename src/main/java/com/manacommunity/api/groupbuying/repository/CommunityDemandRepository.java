package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.CommunityDemand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityDemandRepository extends JpaRepository<CommunityDemand, Long> {
    List<CommunityDemand> findByCommunityIdOrderByUpvotesCountDesc(Long communityId);
}
