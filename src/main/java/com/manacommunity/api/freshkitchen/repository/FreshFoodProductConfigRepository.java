package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import com.manacommunity.api.freshkitchen.model.FreshFoodProductConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshFoodProductConfigRepository extends JpaRepository<FreshFoodProductConfig, Long> {

    Optional<FreshFoodProductConfig> findByProductId(String productId);

    Optional<FreshFoodProductConfig> findByProductIdAndIsActiveTrue(String productId);

    List<FreshFoodProductConfig> findByIsActiveTrue();

    List<FreshFoodProductConfig> findByCategoryAndIsActiveTrue(FoodCategory category);

    List<FreshFoodProductConfig> findByExecutionModeAndIsActiveTrue(ExecutionMode executionMode);

    List<FreshFoodProductConfig> findByCategoryAndExecutionModeAndIsActiveTrue(FoodCategory category, ExecutionMode executionMode);
}
