package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.OpenPlaySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OpenPlaySessionRepository extends JpaRepository<OpenPlaySession, Long> {
    List<OpenPlaySession> findByCommunityIdAndStatus(Long communityId, String status);
    List<OpenPlaySession> findByCommunityId(Long communityId);
}
