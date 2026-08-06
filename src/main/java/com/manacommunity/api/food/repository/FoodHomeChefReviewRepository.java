package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodHomeChefReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodHomeChefReviewRepository extends JpaRepository<FoodHomeChefReview, Long> {

    Page<FoodHomeChefReview> findByChefId(Long chefId, Pageable pageable);
}
