package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodNutritionConsultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodNutritionConsultationRepository extends JpaRepository<FoodNutritionConsultation, Long> {

    Page<FoodNutritionConsultation> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<FoodNutritionConsultation> findByNutritionistId(Long nutritionistId, Pageable pageable);
}
