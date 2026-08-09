package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventMediaDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventMediaDayRepository extends JpaRepository<EventMediaDay, Long> {

    List<EventMediaDay> findByCommunityIdOrderBySortOrderAscLabelAsc(Long communityId);

    long countByCommunityId(Long communityId);
}
