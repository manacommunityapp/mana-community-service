package com.manacommunity.api.communityops.move;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MoveChecklistItemRepository extends JpaRepository<MoveChecklistItem, Long> {
    List<MoveChecklistItem> findByCommunityIdAndMoveTypeOrderByDisplayOrderAsc(Long communityId, MoveType moveType);
    // community_id=0 = global templates
    List<MoveChecklistItem> findByCommunityIdInAndMoveTypeOrderByDisplayOrderAsc(List<Long> ids, MoveType moveType);
}
