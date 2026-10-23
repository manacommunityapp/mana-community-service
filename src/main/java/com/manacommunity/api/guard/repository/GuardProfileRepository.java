package com.manacommunity.api.guard.repository;

import com.manacommunity.api.guard.entity.GuardProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardProfileRepository extends JpaRepository<GuardProfile, Long> {

    List<GuardProfile> findByCommunityIdOrderByFullNameAsc(Long communityId);

    List<GuardProfile> findByCommunityIdAndStatusOrderByFullNameAsc(Long communityId, GuardProfile.GuardStatus status);

    Optional<GuardProfile> findByCommunityIdAndEmployeeId(Long communityId, String employeeId);

    long countByCommunityId(Long communityId);

    long countByCommunityIdAndStatus(Long communityId, GuardProfile.GuardStatus status);
}
