package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.model.PatrolLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrolLogRepository extends JpaRepository<PatrolLog, Long> {

    List<PatrolLog> findByShiftIdOrderByScannedAtAsc(Long shiftId);

    List<PatrolLog> findByCommunityIdOrderByScannedAtDesc(Long communityId);
}
