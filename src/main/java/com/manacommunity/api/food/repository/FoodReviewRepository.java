package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodReviewRepository extends JpaRepository<FoodReview, Long> {

    Page<FoodReview> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Optional<FoodReview> findByEntityTypeAndEntityIdAndUserId(String entityType, Long entityId, Long userId);
}
