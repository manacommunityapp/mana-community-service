package com.manacommunity.api.communityops.move;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MoveChecklistCompletionRepository extends JpaRepository<MoveChecklistCompletion, Long> {
    List<MoveChecklistCompletion> findByMoveRequestId(Long moveRequestId);
    Optional<MoveChecklistCompletion> findByMoveRequestIdAndChecklistItemId(Long moveRequestId, Long checklistItemId);
}
