package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventMediaCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventMediaCategoryRepository extends JpaRepository<EventMediaCategory, Long> {

    List<EventMediaCategory> findByCommunityIdOrderBySortOrderAscNameAsc(Long communityId);
}
