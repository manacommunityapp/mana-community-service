package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaSeva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoojaSevaRepository extends JpaRepository<EventPoojaSeva, Long> {

    List<EventPoojaSeva> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventPoojaSeva> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);

    List<EventPoojaSeva> findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(Long communityId, Long mainEventId);

    Optional<EventPoojaSeva> findByIdAndCommunityId(Long id, Long communityId);
}
