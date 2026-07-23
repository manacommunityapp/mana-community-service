package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {

    List<ResourceCategory> findByCommunityIdAndStatusOrderByDisplayOrderAsc(Long communityId, String status);

    List<ResourceCategory> findByCommunityIdOrderByDisplayOrderAsc(Long communityId);
}
