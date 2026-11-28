package com.manacommunity.api.guardops.repository;

import com.manacommunity.api.guardops.entity.GamificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamificationEventRepository extends JpaRepository<GamificationEvent, Long> {
    List<GamificationEvent> findByGuardIdOrderByEarnedAtDesc(Long guardId);
    List<GamificationEvent> findByCommunityIdOrderByEarnedAtDesc(String communityId);
}
