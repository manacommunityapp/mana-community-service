package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.PoojaSeva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoojaSevaRepository extends JpaRepository<PoojaSeva, Long> {

    List<PoojaSeva> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<PoojaSeva> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
