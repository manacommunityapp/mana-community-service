package com.manacommunity.api.communityops.move;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MoveRequestRepository extends JpaRepository<MoveRequest, Long> {
    List<MoveRequest> findByCommunityIdAndStatus(Long communityId, MoveStatus status);
    List<MoveRequest> findByCommunityId(Long communityId);
    List<MoveRequest> findByResidentUserId(Long residentUserId);
    long countByCommunityIdAndStatus(Long communityId, MoveStatus status);
}
