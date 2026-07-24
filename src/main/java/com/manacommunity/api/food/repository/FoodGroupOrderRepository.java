package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroupOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodGroupOrderRepository extends JpaRepository<FoodGroupOrder, Long> {

    Optional<FoodGroupOrder> findByJoinCode(String joinCode);

    Page<FoodGroupOrder> findByCreatedByIdAndCommunityId(Long createdById, Long communityId, Pageable pageable);
}
