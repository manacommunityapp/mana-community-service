package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.PoojaSeva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoojaSevaRepository extends JpaRepository<PoojaSeva, Long> {

    List<PoojaSeva> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<PoojaSeva> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);

    List<PoojaSeva> findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(Long communityId, Long mainEventId);

    Optional<PoojaSeva> findByIdAndCommunityId(Long id, Long communityId);
}
