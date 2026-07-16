package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityLeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityLeaderRepository extends JpaRepository<CommunityLeader, Long> {

    List<CommunityLeader> findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(Long communityId);

    List<CommunityLeader> findByCommunityIdOrderByDisplayOrderAsc(Long communityId);

    boolean existsByCommunityIdAndUserIdAndDesignation(Long communityId, Long userId, String designation);
}
