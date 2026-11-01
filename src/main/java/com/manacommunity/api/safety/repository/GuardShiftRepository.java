package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.model.GuardShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository("safetyGuardShiftRepository")
public interface GuardShiftRepository extends JpaRepository<GuardShift, Long> {

    List<GuardShift> findByCommunityIdOrderByStartTimeDesc(Long communityId);
}
