package com.manacommunity.api.opsresilience.repository;

import com.manacommunity.api.opsresilience.entity.GuardExpressCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardExpressCheckinRepository extends JpaRepository<GuardExpressCheckin, Long> {
    Optional<GuardExpressCheckin> findByGateIdAndClientSeqId(String gateId, Long clientSeqId);
    List<GuardExpressCheckin> findByCommunityIdAndRequiresMcReviewTrueOrderByCreatedAtDesc(Long communityId);
}
