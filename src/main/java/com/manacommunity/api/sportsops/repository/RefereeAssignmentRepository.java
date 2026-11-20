package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.RefereeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefereeAssignmentRepository extends JpaRepository<RefereeAssignment, Long> {
    List<RefereeAssignment> findByCommunityId(Long communityId);
    List<RefereeAssignment> findByCommunityIdAndRefereeId(Long communityId, Long refereeId);
    List<RefereeAssignment> findByCommunityIdAndEventId(Long communityId, Long eventId);
}
