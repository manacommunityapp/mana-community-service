package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodAiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodAiRecommendationRepository extends JpaRepository<FoodAiRecommendation, Long> {

    List<FoodAiRecommendation> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<FoodAiRecommendation> findByUserIdAndRecommendationType(Long userId, String recommendationType);
}
