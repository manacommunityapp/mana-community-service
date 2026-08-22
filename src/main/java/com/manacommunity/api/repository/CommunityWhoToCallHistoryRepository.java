package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityWhoToCallHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityWhoToCallHistoryRepository extends JpaRepository<CommunityWhoToCallHistory, Long> {

    List<CommunityWhoToCallHistory> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<CommunityWhoToCallHistory> findByWhoToCallIdOrderByCreatedAtDesc(Long whoToCallId);
}
