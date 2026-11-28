package com.manacommunity.api.guardops.repository;

import com.manacommunity.api.guardops.entity.GuardProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuardProfileRepository extends JpaRepository<GuardProfile, Long> {
    List<GuardProfile> findByCommunityIdAndIsActiveTrue(String communityId);
    List<GuardProfile> findByAssignedGateId(String gateId);
}
