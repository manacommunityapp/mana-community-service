package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodDiningEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodDiningEventRepository extends JpaRepository<FoodDiningEvent, Long> {

    Page<FoodDiningEvent> findByCommunityIdAndStatus(Long communityId, FoodDiningEvent.DiningEventStatus status, Pageable pageable);

    Optional<FoodDiningEvent> findByIdAndCommunityId(Long id, Long communityId);
}
