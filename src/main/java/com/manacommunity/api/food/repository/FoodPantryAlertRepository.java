package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodPantryAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodPantryAlertRepository extends JpaRepository<FoodPantryAlert, Long> {

    List<FoodPantryAlert> findByUserIdAndIsRead(Long userId, Boolean isRead);

    List<FoodPantryAlert> findByUserIdAndCommunityId(Long userId, Long communityId);
}
