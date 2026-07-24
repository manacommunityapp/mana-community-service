package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodEventRepository extends JpaRepository<FoodEvent, Long> {

    Page<FoodEvent> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);

    Optional<FoodEvent> findByIdAndCommunityId(Long id, Long communityId);
}
