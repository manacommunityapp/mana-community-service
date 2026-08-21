package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityWhoToCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityWhoToCallRepository extends JpaRepository<CommunityWhoToCall, Long> {

    List<CommunityWhoToCall> findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(Long communityId);

    List<CommunityWhoToCall> findByCommunityIdOrderByDisplayOrderAsc(Long communityId);
}
