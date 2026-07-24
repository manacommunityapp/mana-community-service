package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCaterer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodCatererRepository extends JpaRepository<FoodCaterer, Long> {

    Page<FoodCaterer> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);

    Optional<FoodCaterer> findByIdAndCommunityId(Long id, Long communityId);
}
