package com.manacommunity.api.guardops.repository;

import com.manacommunity.api.guardops.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {
    List<ShiftHandover> findByCommunityIdOrderByHandoverInitiatedAtDesc(String communityId);
    List<ShiftHandover> findByGateIdAndStatusNot(String gateId, ShiftHandover.HandoverStatus status);
}
