package com.manacommunity.api.wellnessbasket.repository;

import com.manacommunity.api.wellnessbasket.entity.WellnessProductNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WellnessProductNutritionRepository extends JpaRepository<WellnessProductNutrition, Long> {

    Optional<WellnessProductNutrition> findByProductId(Long productId);

    List<WellnessProductNutrition> findByProductIdIn(Collection<Long> productIds);
}
