package com.manacommunity.api.emergency.repository;

import com.manacommunity.api.emergency.entity.EvacuationBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvacuationBroadcastRepository extends JpaRepository<EvacuationBroadcast, Long> {
    List<EvacuationBroadcast> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
